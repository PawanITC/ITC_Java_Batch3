# notification-service

Event-driven service that reacts to order and payment events and delivers notifications through two channels: in-app (persisted to a `notifications` table and served via REST) and email (sent via SMTP / JavaMailSender).

## Responsibilities

- Consume order lifecycle events from Kafka
- Persist in-app notifications per user
- Send transactional emails via SMTP (Gmail)
- Expose notifications to the frontend via REST (list, mark read)
- (Stubbed) SMS via Twilio — wired up with Resilience4j retry but using placeholder credentials

---

## REST Endpoints

All routes prefixed at `/api/v1` by the API Gateway.

### Notifications (`/notifications`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/notifications` | Authenticated | Returns all notifications for the calling user, newest first. Frontend polls this every 10 s to drive the bell icon badge. |
| PATCH | `/notifications/{id}/read` | Authenticated | Marks a single notification as read. |
| PATCH | `/notifications/read-all` | Authenticated | Marks all of the user's notifications as read. |
| POST | `/notifications/order-event` | Internal | Receives an order event payload and creates a notification + sends email. Can be called directly in tests; in production this is driven by the Kafka consumer. |

---

## How Each Feature Works

### Kafka consumer
`OrderEventConsumer` (internal to notification-service) listens on `orders.events.v1` and `payments.events.v1`. The consumer deserialises incoming records as `Map<String, Object>` (string deserialiser, no type headers required) and routes on the `eventType` field.

For each event the consumer calls `NotificationService.processOrderEvent(event)` which:
1. Builds a human-readable title and message for the event type (e.g. "Order Confirmed", "Payment Failed").
2. Persists a `Notification` row for the `userId` extracted from the event.
3. Calls `EmailService.sendOrderNotificationEmail(toAddress, subject, body)`.

**consumer group**: `notification-service`  
**auto-offset-reset**: `earliest`  
**deserialiser**: `StringDeserializer` (raw string, no type mapping needed since the service doesn't need the full typed DTO — it only reads `eventType`, `orderId`, and `userId` from the map).

### In-app notifications
The `Notification` entity has fields: `id`, `userId`, `title`, `message`, `type` (enum), `isRead`, `createdAt`. The REST layer filters by `userId` extracted from the `X-User-Id` gateway header.

The frontend `useNotifications` hook fetches via React Query with a 10-second polling interval. The bell icon in the header shows a count badge of unread notifications. Clicking any notification calls `PATCH /{id}/read`.

### Email delivery
`EmailService` uses Spring's `JavaMailSender` configured against Gmail SMTP (port 587, STARTTLS). The `MAIL_USERNAME` and `MAIL_PASSWORD` environment variables must be set with a real Gmail address and an App Password (2FA must be enabled on the Google account). In Docker the `.env` file supplies these; they default to placeholder values which make email delivery silently fail without crashing the service.

Resilience4j `emailRetry` instance: max 3 attempts, 2-second wait between retries.

### SMS (Twilio — stubbed)
The Twilio client is wired but the credentials in `.env` are placeholders. The `smsRetry` Resilience4j instance wraps the SMS call. SMS delivery will fail silently until real `TWILIO_SID`, `TWILIO_AUTHTOKEN`, and `TWILIO_FROMNUMBER` values are provided.

---

## Kafka

| Topic | Direction | Events consumed | Action |
|-------|-----------|----------------|--------|
| `orders.events.v1` | Consumer | `ORDER_INITIATED`, `PAYMENT_SUCCESS`, `ORDER_SHIPPED`, `ORDER_DELIVERED`, `ORDER_CANCELLED` | Persist notification + send email |
| `payments.events.v1` | Consumer | `PAYMENT_FAILED` | Persist notification + send email |

---

## Storage

**PostgreSQL** — `mydb` (shared database)

The JDBC URL was originally hardcoded to `notificationDB` which does not exist in the Docker PostgreSQL container. It is now `${DB_NAME:mydb}` with `DB_NAME=mydb` set in `docker-compose.yml`.

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| user_id | BIGINT | Owner |
| title | VARCHAR | Short heading, e.g. "Order Confirmed" |
| message | VARCHAR | Full human-readable description |
| type | VARCHAR | e.g. `ORDER_UPDATE`, `PAYMENT_UPDATE` |
| is_read | BOOLEAN | Defaults false |
| created_at | TIMESTAMP | |

---

## Configuration Notes

| Env var | Default | Purpose |
|---------|---------|---------|
| `POSTGRES_HOST` | `localhost` | DB host; Docker sets `postgres-db` |
| `DB_NAME` | `mydb` | DB name; must match the postgres container's `POSTGRES_DB` |
| `KAFKA_HOST` | `localhost` | Kafka broker host |
| `KAFKA_PORT` | `29092` | Local dev port; Docker sets `9092` |
| `MAIL_USERNAME` | placeholder | Gmail address |
| `MAIL_PASSWORD` | placeholder | Gmail App Password |
| `TWILIO_SID` | placeholder | Twilio account SID |

---

## Observability

- OTEL traces exported via Micrometer OTLP HTTP to the collector on port 4318.
- `OTEL_SERVICE_NAME=notification-service` and `-Dotel.service.name=notification-service` set in docker-compose so Jaeger displays the correct service name instead of `unknown_service` (which occurs when the OTEL SDK initialises before Spring's application context binds `spring.application.name`).
- Log pattern includes `traceId` and `spanId` from MDC.
