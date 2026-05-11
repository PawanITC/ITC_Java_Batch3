# order-service

The saga orchestrator. Creates and tracks orders. Reacts to payment outcomes via Kafka and coordinates the full order lifecycle from initiation through to delivery.

## Responsibilities

- Consume `CHECKOUT_INITIATED` Kafka events and persist `Order` rows
- Expose order history and detail to authenticated users
- Admin view: list all orders, update order status manually
- Handle order cancellation with compensation event
- Consume payment events (`PAYMENT_SUCCESS`, `PAYMENT_FAILED`) and update order status accordingly
- Publish order state-change events for notification-service and other downstream consumers

---

## REST Endpoints

All routes prefixed at `/api/v1` by the API Gateway.

### User orders (`/orders`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/orders/{id}` | Authenticated | Single order detail. Returns line items, status, total. |
| GET | `/orders/history` | Authenticated | All orders for the calling user, newest first. Frontend polls this every 3 s while any order is `PENDING`. |
| PATCH | `/orders/{id}/cancel` | Authenticated | Cancel a PENDING order. Publishes `ORDER_CANCELLED` compensation event. |

### Admin orders (`/admin/orders`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/admin/orders` | ADMIN | Paginated list of all orders across all users. |
| GET | `/admin/orders/{id}` | ADMIN | Full order detail including user info. |
| PATCH | `/admin/orders/{id}/status` | ADMIN | Manually override order status (e.g. mark as SHIPPED, DELIVERED). |

---

## How Each Feature Works

### Order creation (Kafka-driven)
order-service does **not** expose a `POST /orders` REST endpoint. Orders are created entirely via Kafka:

1. product-service publishes `CheckoutInitiatedEvent` to `checkout.events.v1`.
2. `OrderEventConsumer.handleCheckoutInitiated()` receives it.
3. `OrderService.processOrderInitiation(event)` persists an `Order` with status `PENDING`, line items, and total.
4. `KafkaEventServiceImpl.sendOrderCreated()` publishes `OrderEvent{ORDER_INITIATED}` to `orders.events.v1` using the **afterCommit hook** — Kafka does not receive the event until the DB transaction successfully commits.

The afterCommit pattern is critical: without it, a DB rollback after Kafka publish would leave a phantom event that payment-service would try to process for a non-existent order.

### Payment saga — happy path
```
checkout.events.v1  →  order-service creates Order{PENDING}
orders.events.v1    →  ORDER_INITIATED  →  payment-service processes payment
payments.events.v1  →  PAYMENT_SUCCESS  →  order-service updates Order to PAID
orders.events.v1    →  PAYMENT_SUCCESS  →  notification-service sends confirmation email
```

`handlePaymentOutcome()` in `OrderEventConsumer` consumes `payments.events.v1`:
- `PAYMENT_SUCCESS` → `orderService.updateOrderStatus(orderId, PAID)`
- `PAYMENT_FAILED` → `orderService.updateOrderStatus(orderId, FAILED)`
- `ORDER_REFUNDED` → `orderService.updateOrderStatus(orderId, REFUNDED)`

### Idempotency guard
`updateOrderStatus()` checks whether the order already has the target status before writing:
```java
if (order.getStatus() == newStatus) {
    return mapper.toResponse(order);  // no-op
}
```
This makes the consumer safe under Kafka's at-least-once delivery — redelivered events do not trigger duplicate status changes or duplicate downstream events.

### Header resilience
`handlePaymentOutcome` uses `@Header(value = "event_type", required = false)`. If the `event_type` Kafka header is absent (e.g. on manually produced test messages), the consumer falls back to the `eventType` field in the JSON payload. Without `required = false`, a missing header causes a deserialization exception before the method body runs; because the consumer uses `MANUAL_IMMEDIATE` ack mode, the message is never acknowledged, blocking the consumer partition indefinitely.

Unknown event types log a warning and ACK immediately to unblock the consumer.

### Cancellation (compensation)
`PATCH /orders/{id}/cancel` checks the order belongs to the calling user and is still in a cancellable state (PENDING). It:
1. Sets order status to CANCELLED.
2. Publishes `OrderCancelledEvent` to a dedicated topic for downstream services to react (e.g. restore stock, void the payment intent).

---

## Kafka

| Topic | Direction | Event type | Trigger |
|-------|-----------|-----------|---------|
| `checkout.events.v1` | Consumer | `CheckoutInitiatedEvent` | product-service checkout |
| `orders.events.v1` | Producer | `ORDER_INITIATED` | Order row created |
| `orders.events.v1` | Producer | `PAYMENT_SUCCESS` | Payment confirmed |
| `orders.events.v1` | Producer | `ORDER_CANCELLED` | User cancels order |
| `orders.events.v1` | Producer | `ORDER_SHIPPED`, `ORDER_DELIVERED` | Admin status update |
| `payments.events.v1` | Consumer | `PAYMENT_SUCCESS` / `PAYMENT_FAILED` / `ORDER_REFUNDED` | payment-service outcome |

**Consumer group**: `order-service-group-v2`  
**auto-offset-reset**: `earliest` — on a fresh group start the consumer replays the full topic, ensuring no events are missed after restarts.

---

## Storage

**PostgreSQL** — `order_db`

| Table | Key columns | Notes |
|-------|------------|-------|
| `orders` | id, customer_id, status, total_amount, created_at | Status: PENDING → PAID → SHIPPED → DELIVERED (or FAILED / CANCELLED / REFUNDED) |
| `order_items` | id, order_id, product_id, product_name, quantity, price | Snapshot of product at order time |

Status is stored as a `VARCHAR` / enum column. Hibernate `ddl-auto: update` manages the schema in the dev profile.

---

## Security

- `/orders/**` requires authenticated JWT (user extracted from `X-User-Id` gateway header).
- Ownership check: every user-facing query filters by `customerId = requestingUserId`.
- `/admin/orders/**` requires `ROLE_ADMIN`.

---

## Observability

- OTEL traces include Kafka consumer and producer spans.
- `traceparent` header propagated through Kafka records so the full checkout → payment → notification chain appears as a single trace in Jaeger.
- Consumer group: `order-service-group-v2` (bumped from v1 after fixing the group ID mismatch that caused messages to be skipped).
