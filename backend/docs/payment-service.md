# payment-service

Saga participant responsible for processing payments via Stripe. Consumes order events, creates PaymentIntents, confirms charges, and publishes payment outcomes back to the event bus.

## Responsibilities

- Listen for `ORDER_INITIATED` events and create Stripe PaymentIntents
- Expose the PaymentIntent `clientSecret` to the frontend for card collection
- Confirm payments server-side with idempotency
- Handle Stripe webhooks as the authoritative source of truth for payment status
- Fall back to inline success handling when webhooks are unavailable (dev/Docker)
- Process refunds
- Publish `PAYMENT_SUCCESS`, `PAYMENT_FAILED`, and `ORDER_REFUNDED` events

---

## REST Endpoints

All routes prefixed at `/api/v1` by the API Gateway.

### Payments (`/payments`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/payments/create-intent` | Authenticated | Creates a Stripe PaymentIntent and a local `Payment` row. Returns `clientSecret` and `paymentIntentId`. |
| POST | `/payments/confirm` | Authenticated | Confirms the PaymentIntent with Stripe. In production, status moves to PROCESSING and the webhook finalises it. In dev/Docker (no webhook delivery), the confirmed intent is checked immediately and `handlePaymentSuccess` is called inline if Stripe reports `"succeeded"`. |
| GET | `/payments/my-latest` | Authenticated | Returns the latest PaymentIntent for the user (used by the checkout page on reload/restore). Returns 204 if none exists. |
| GET | `/payments/{paymentId}` | Authenticated | Fetch a specific payment record. Ownership enforced. |
| POST | `/payments/{paymentId}/refund` | Authenticated | Full refund via Stripe. Only SUCCEEDED payments can be refunded. Idempotent. |
| POST | `/payments/webhook` | Public (Stripe sig) | Stripe webhook receiver. Verifies `Stripe-Signature` header, routes `payment_intent.succeeded`, `payment_intent.payment_failed`, and `charge.refunded` events. Source of truth in production. |

---

## How Each Feature Works

### PaymentIntent creation
1. `ORDER_INITIATED` Kafka event arrives → `PaymentOrderEventConsumer.handleOrderInitiated()`.
2. `PaymentService.createPaymentIntent(event)` is called.
3. Creates a local `Payment` row in PENDING state with `orderId`, `userId`, and `amount`.
4. Calls `StripeService.createPaymentIntent(amount, currency, ...)` with a stable idempotency key derived from the local payment ID — safe to retry without double-charging.
5. Stores the Stripe `paymentIntentId` on the `Payment` entity.

### Frontend checkout flow
```
POST /payments/create-intent  →  { clientSecret, paymentIntentId }
  ↓
Frontend uses Stripe Elements to collect card (clientSecret renders the card form)
  ↓
POST /payments/confirm  →  { paymentIntentId, paymentMethodId, returnUrl }
  ↓
Backend calls Stripe to confirm  →  checks intent.status
  ↓  if "succeeded"             → handlePaymentSuccess() inline → PAYMENT_SUCCESS on Kafka
  ↓  if "processing"/"requires_action"  → stays PROCESSING, waits for webhook
```

### Inline success handling (dev/Docker fix)
`StripeService.confirmPaymentIntent()` returns the confirmed `PaymentIntent` object instead of `void`. `PaymentService.confirmPayment()` reads `intent.getStatus()`:

```java
if ("succeeded".equals(intentStatus)) {
    handlePaymentSuccess(confirmedIntent);  // publishes PAYMENT_SUCCESS immediately
} else {
    payment.setStatus(STATUS_PROCESSING);   // wait for webhook
}
```

This is essential in environments where Stripe webhooks cannot reach the service (local Docker, no public tunnel). `handlePaymentSuccess()` is idempotent — if a webhook also arrives later it detects `status == SUCCEEDED` and no-ops.

### Webhook handling (production)
`POST /payments/webhook` verifies the `Stripe-Signature` header using the webhook signing secret. Verified events are routed:

| Stripe event | Handler | Effect |
|-------------|---------|--------|
| `payment_intent.succeeded` | `handlePaymentSuccess(intent)` | Sets Payment to SUCCEEDED, publishes `PAYMENT_SUCCESS` |
| `payment_intent.payment_failed` | `handlePaymentFailure(intent)` | Sets Payment to FAILED, publishes `PAYMENT_FAILED` |
| `charge.refunded` | `handlePaymentRefunded(charge)` | Sets Payment to REFUNDED, publishes `ORDER_REFUNDED` |

Each handler has a guard: if the Payment is already in the target state it returns immediately without re-publishing.

### Idempotency
- PaymentIntent creation uses a Stripe idempotency key = `"pi:create:{paymentId}"`. Retried requests return the same intent.
- Confirmation uses `"pi:confirm:{piId}:{paymentId}"`.
- All terminal-state transitions are guarded: `isFinalState(status)` returns true for SUCCEEDED / FAILED / REFUNDED, preventing downgrade.

### `my-latest` endpoint
Returns the most recent Payment for the authenticated user. `getLatestPaymentIntent()` calls `paymentRepository.findTopByUserIdOrderByCreatedAtDesc(userId)`, then retrieves the live `PaymentIntent` from Stripe to return the current `clientSecret` (secrets can expire, so a fresh retrieval is necessary). Returns `Optional.empty()` → 204 when no payment exists, rather than throwing 404.

---

## Kafka

| Topic | Direction | Event | Trigger |
|-------|-----------|-------|---------|
| `orders.events.v1` | Consumer | `ORDER_INITIATED` | order-service creates new order |
| `payments.events.v1` | Producer | `PAYMENT_SUCCESS` | Payment confirmed (webhook or inline) |
| `payments.events.v1` | Producer | `PAYMENT_FAILED` | Payment declined |
| `payments.events.v1` | Producer | `ORDER_REFUNDED` | Stripe refund processed |

**Consumer group**: `payment-service-group-v6`  
**auto-offset-reset**: `earliest`

All Kafka publishes use `ProducerRecord` with an `event_type` header set to the enum name — e.g. `"PAYMENT_SUCCESS"`. The message key is the `paymentId` (ensures partition affinity for the same payment).

---

## Storage

**PostgreSQL** — `payment_db`

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | Internal payment ID |
| order_id | BIGINT | Foreign reference to order-service |
| user_id | BIGINT | Owner |
| stripe_payment_intent_id | VARCHAR | `pi_...` from Stripe |
| idempotency_key | VARCHAR UNIQUE | UUID generated at creation, reused for Stripe requests |
| amount | BIGINT | In smallest currency unit (pence/cents) |
| currency | VARCHAR | ISO code, e.g. `"gbp"` |
| status | VARCHAR | `PENDING`, `PROCESSING`, `succeeded`, `failed`, `refunded` |
| created_at | TIMESTAMP | |

---

## Security

- Webhook endpoint is public but protected by Stripe signature verification (`Webhook.constructEvent`).
- All other endpoints require JWT.
- Ownership is enforced on `getPayment` and `refundPayment` by comparing `payment.getUserId()` to the requesting user's ID.

---

## Observability

- OTEL spans cover HTTP endpoints and Kafka producer calls.
- Stripe API calls appear as child spans within the confirmation trace.
- `traceparent` Kafka header propagated so the full order saga is visible as one trace in Jaeger.
