# Funkart E-Commerce Platform — Full Project Retrospective

> A chronological record of every feature built, every bug fixed, and every architectural decision made during the development of the Funkart microservices e-commerce platform.

---

## Project Overview

Funkart is a full-stack e-commerce application built on a Java Spring Boot microservices backend and a React/Vite frontend. The backend follows a **saga pattern** for distributed transactions, with Apache Kafka as the event bus, PostgreSQL for persistence, Redis for caching/rate-limiting, and OpenTelemetry for distributed tracing.

### Architecture at a glance

```
React Frontend (Vite + React Query)
        │
        ▼
   API Gateway  ──── Redis (rate limit / session)
        │
        ├── user-service          (auth, JWT, OAuth2, profiles)
        ├── product-service       (catalogue, inventory, admin)
        ├── order-service         (saga orchestrator)
        ├── payment-service       (Stripe / mock, saga participant)
        ├── notification-service  (email via SMTP)
        ├── review-rating-service (review write-side)
        └── rating-aggregator-service (review read-side + Redis cache)
                       │
               Apache Kafka (event bus)
                       │
            PostgreSQL (per-service DB)
                       │
          OpenTelemetry → Jaeger / OTLP collector
```

---

## Phase 1 — Initial Audit and Cart Fix

### Task 1 · Codebase Audit

The session began with a full audit of the existing codebase generated from base44 (a frontend scaffold tool). The frontend had all the UI pages wired with React Query hooks pointing at a REST API, but the backend microservices had not been mapped to match those query paths. Several mismatches were identified:

- Frontend called `/api/v1/cart/update` with a delta value; backend expected an absolute quantity.
- OAuth callback was hitting a 403 because `SecurityConfig` in `user-service` blocked the `/oauth2/**` path.
- Admin user endpoints returned 500 due to missing serialisation config on the `User` entity.
- Payment flow sent a `my-latest` request on every render due to missing query deduplication.
- Logout did not clear the JWT cookie, leaving the session alive.

### Task 2 · Cart +/- Button Fix

**Problem:** The frontend sent `+1` or `-1` as the quantity delta, but `CartController.updateItem()` treated the incoming value as the *new absolute* quantity. Clicking "+" twice would set quantity to 1 both times.

**Fix:** `CartService.updateQuantity()` was changed to accept a delta and compute `currentQty + delta`, with a guard to remove the item if the result fell to zero or below. The frontend hook `useUpdateCartItem` was left unchanged.

---

## Phase 2 — Authentication and Security

### Task 3 · GitHub OAuth Callback 403

**Problem:** Spring Security's filter chain in `user-service` listed `/oauth2/callback/**` under `permitAll()`, but the actual callback path registered with GitHub was `/login/oauth2/code/github`. These paths did not match, causing a 403 on every OAuth login attempt.

**Fix:** `SecurityConfig.java` was updated to permit `/login/oauth2/**` and `/oauth2/**`. The `OAuth2LoginSuccessHandler` was also wired correctly so that after GitHub redirects back, the handler exchanges the OAuth2 principal for a local JWT, sets an HTTP-only cookie, and redirects the browser to the frontend dashboard.

**Architecture note:** The `getOrCreateOAuthUser(email, name)` method in `UserService` is the idempotency gate — it creates a user on first OAuth login and returns the existing one on subsequent logins. The password column is set to the sentinel value `{OAUTH}` for accounts that have no password.

### Task 7 · Logout/Login Session Flow

**Problem:** After clicking logout, refreshing the page still showed the user as logged in. The JWT cookie was not being cleared.

**Fix:** `AuthController.logout()` was updated to issue a `Set-Cookie` response header with `Max-Age=0`, which instructs the browser to immediately delete the JWT cookie. React Query's cache was invalidated client-side in the `useLogout` hook by calling `queryClient.clear()`.

---

## Phase 3 — Admin Panels

### Task 4 · Admin Users 500 Error

**Problem:** `GET /api/v1/admin/users` returned 500. The `User` entity had a bi-directional JPA relationship with lazy-loaded collections. When Jackson tried to serialise the entity list, it triggered a `LazyInitializationException` because the Hibernate session had already closed.

**Fix:** `UserService.findAllUsers()` was annotated with `@Transactional(readOnly = true)` to keep the session open during serialisation. A `UserAdminDto` projection was introduced so Jackson only serialised safe flat fields (id, name, email, role, isActive) rather than the full entity graph.

### Task 5 · Product Admin Frontend Page

A full admin product management page was built with:
- A paginated product table with search
- An "Add Product" modal with form validation
- Edit and delete actions wired to `product-service` endpoints
- Optimistic UI updates via React Query `useMutation`

---

## Phase 4 — Payment Flow and Order Lifecycle

### Task 6 · Payment Flow `my-latest` 400 Spam

**Problem:** The `useLatestOrder` hook called `GET /api/v1/orders/my-latest` on every render with no deduplication. Because this endpoint returned 404 when the user had no orders, the frontend was logging hundreds of 400/404 errors per session.

**Fix:** The query was wrapped with `enabled: !!userId` and given a `staleTime` of 60 seconds so it would only re-fetch after data aged out. A 404 was also made non-throwing — the hook returned `null` rather than propagating the error.

---

## Phase 5 — UX Improvements

### Task 8 · Toast Notifications — Bottom-Left with Swipe-to-Dismiss

The default Shadcn/ui Toaster was reconfigured:
- `position` moved from top-right to bottom-left
- Swipe direction set to `left`
- Duration reduced to 4 seconds for transient confirmations

### Task 9 · ROLE_MODERATOR for Product Review Moderation

**Added a new role tier:**
- `ROLE_MODERATOR` was added to the `Role` enum in `user-service`
- `SecurityConfig` in `product-service` and `review-rating-service` was updated so that moderators can approve/reject reviews but cannot access the full admin panel
- The frontend admin users page was updated with a tri-state role selector (User / Moderator / Admin)
- The admin users page avatar colour scheme: blue = User, amber = Moderator, accent = Admin

---

## Phase 6 — Notification Service

### Task 10 · Real Order Notifications

**Problem:** The notification bell in the header showed placeholder text regardless of order state.

**Fix:** A `NotificationService` consumer was wired to listen on the `orders.events.v1` Kafka topic. When an `OrderEvent` arrives (ORDER_INITIATED, PAYMENT_SUCCESS, ORDER_SHIPPED, etc.), the service persists a `Notification` row and optionally sends an email via SMTP. The frontend `useNotifications` hook polls `GET /api/v1/notifications` every 10 seconds and marks items read via `PATCH /api/v1/notifications/{id}/read`.

---

## Phase 7 — Observability

### Task 11 · OpenTelemetry Distributed Tracing

OpenTelemetry (OTEL) was added to all microservices:

- `opentelemetry-spring-boot-starter` added to each service's `build.gradle`
- `application.yaml` in every service was updated with:
  ```yaml
  management:
    tracing:
      sampling:
        probability: 1.0
  otel:
    exporter:
      otlp:
        endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://otel-collector:4318}
  ```
- Trace context propagates through Kafka headers (`traceparent` / `tracestate`) so a single user action (e.g., place order → payment → notification) produces a single connected trace in Jaeger
- The API Gateway was configured to forward OTEL headers downstream so the root span originates at the gateway

---

## Phase 8 — Testing

### Task 12 · Microservice Test Coverage Audit and Improvement

Each service's test suite was reviewed and strengthened:

**user-service:** Added `@WebMvcTest` slice tests for `AuthController`, `UserController`, and `AdminController`. Mocked `UserService` and `JwtService`. Covered: signup 201, login 200, profile fetch, logout cookie clearing, role change, toggle active.

**product-service:** Added tests for `ProductController` paginated list and admin create/update/delete. Verified `@PreAuthorize("hasRole('ADMIN')")` guards return 403 for regular users.

**order-service:** Added `@SpringBootTest` integration test for the saga happy path. Verified order moves from PENDING → PAID when a `PAYMENT_SUCCESS` Kafka event arrives.

**payment-service:** Added unit tests for `PaymentService.processPayment()` covering success, insufficient funds, and Stripe timeout scenarios.

---

## Phase 9 — Shared Library Documentation

### Task 13 · common-contracts Javadoc

`common-contracts` is the shared library JAR consumed by all microservices. It contains:
- Event DTOs (`OrderEvent`, `PaymentEvent`, `ReviewEvent`, `OrderCancelledEvent`)
- Cross-service DTO projections (`UserProfileDto`, `UserDto`)
- Enums (`OrderEventType`, `OrderStatus`, `PaymentStatus`)

All Javadoc warnings were resolved. Every public type now has a class-level `@since` tag and parameter documentation.

---

## Phase 10 — Review and Rating System

### Tasks 14–20 · Review/Rating Microservices (Design, Scaffold, Fix)

This was the most substantial feature addition: a full event-sourced review system split across two services.

#### Task 14 · ReviewEvent in common-contracts

Added to `common-contracts`:
```java
public record ReviewEvent(
    Long reviewId,
    Long productId,
    Long userId,
    int rating,           // 1–5
    String body,
    ReviewEventType type  // REVIEW_SUBMITTED, REVIEW_APPROVED, REVIEW_REJECTED
) {}
```

Also added `review.events.v1` as a new Kafka topic constant.

#### Task 15 · review-rating-service (Write Side)

Scaffolded `review-rating-service` as a full Spring Boot microservice:

- **Domain:** `Review` entity with fields: productId, userId, rating, body, status (PENDING / APPROVED / REJECTED)
- **REST API:**
  - `POST /api/v1/reviews` — authenticated user submits a review
  - `GET /api/v1/reviews/product/{productId}` — list approved reviews for a product
  - `PATCH /api/v1/reviews/{id}/approve` — moderator approves
  - `PATCH /api/v1/reviews/{id}/reject` — moderator rejects
- **Kafka producer:** publishes `ReviewEvent{REVIEW_SUBMITTED}` after every new review
- **SecurityConfig:** JWT filter applied, public read endpoints, authenticated write, moderator-only moderation paths

#### Task 16 · rating-aggregator-service (Read Side)

Scaffolded `rating-aggregator-service` as a CQRS read model:

- **Kafka consumer:** listens on `review.events.v1`, updates `RatingAggregate` table (productId → count, sum, average)
- **Redis cache:** `GET /api/v1/ratings/{productId}` served from Redis with 5-minute TTL; cache invalidated on new `REVIEW_APPROVED` event
- **REST API:**
  - `GET /api/v1/ratings/{productId}` — returns `{ productId, averageRating, reviewCount }`
  - `GET /api/v1/ratings/top` — returns top-10 products by average rating

#### Tasks 17–20 · Fixes on Review Services

Several issues were found during compilation and test runs:

**Task 17:** `rating-aggregator-service` had a model mismatch — the Kafka consumer expected a `RatingAggregate` entity with a `productId` primary key, but the initial scaffold used a surrogate `id` column. Fixed by aligning the entity with the test expectations and using `productId` as the natural key with an `@Id` annotation.

**Task 18:** `review-rating-service` was missing `SecurityConfig.java` entirely. The service started but all endpoints defaulted to Spring Security's default (form login), causing 302 redirects instead of 401 JSON responses. A proper `SecurityConfig` was added matching the pattern from `user-service`.

**Task 19:** Both services had nested sub-packages (`impl/`, `dto/request/`, `dto/response/`) that caused import resolution errors because `@ComponentScan` was not configured to scan them. The fix was to flatten to single-level packages and fix all import paths. The `application.yaml` for `review-rating-service` was also missing the `spring.application.name` property.

**Task 20:** `rating-aggregator-service` had leftover scaffold files from an earlier iteration that conflicted with the new implementation. All old models, repositories, and consumers were deleted. The test resource YAML was renamed from `application-test.yaml` to `application.yaml` so `@SpringBootTest` picked it up without needing a `@ActiveProfiles("test")` annotation.

---

## Phase 11 — Kafka Saga Debugging (Orders Stuck PENDING)

This was the most complex debugging session in the project. After end-to-end testing, orders were completing payment successfully but staying in `PENDING` status indefinitely.

### Root cause analysis

The saga flow for placing an order is:

```
Frontend → POST /api/v1/orders
         → order-service creates Order{PENDING}
         → publishes OrderEvent{ORDER_INITIATED} to orders.events.v1
         → payment-service consumes OrderEvent
         → payment-service calls Stripe / mock
         → payment-service publishes PaymentEvent{PAYMENT_SUCCESS} to payments.events.v1
         → order-service consumes PaymentEvent → updates Order to PAID
         → order-service publishes OrderEvent{PAYMENT_SUCCESS} to orders.events.v1
         → notification-service consumes → sends email
```

Three separate bugs were found, each of which could independently cause the order to stay PENDING:

#### Bug 1 — Consumer Group ID Mismatch

`KafkaGroups.java` in `common-contracts` defined:
```java
public static final String ORDER_SERVICE_GROUP  = "order-service-group-v2";
public static final String PAYMENT_SERVICE_GROUP = "payment-service-group-v6";
```

But `application.yaml` in both services still had stale values:
```yaml
# order-service — wrong:
group-id: order-service-group-v1
# payment-service — wrong:
group-id: payment-service-group-v5
```

The `@KafkaListener` annotation uses the YAML value as the default, meaning both consumers joined wrong consumer groups. Since Kafka tracks offsets per group, the new `v2`/`v6` groups had no committed offsets and defaulted to reading only *new* messages (because `auto-offset-reset` defaulted to `latest`). Any message published before the correct group first connected was permanently skipped.

**Fix:** Updated both YAMLs to match the constants. Added `auto-offset-reset: earliest` so fresh groups replay from the beginning of the topic on first startup.

#### Bug 2 — Required Kafka Header Blocking Consumer

`OrderEventConsumer.handlePaymentOutcome()` had:
```java
@Header("event_type") byte[] eventTypeBytes
```

Spring Kafka's `@Header` is `required = true` by default. Any message on the `payments.events.v1` topic that lacked the `event_type` header caused a deserialization exception before the method body was even entered. Because the consumer used `MANUAL_IMMEDIATE` ack mode, a failed-before-ACK message was retried forever, permanently blocking the consumer from advancing to the next offset.

**Fix:**
```java
@Header(value = "event_type", required = false) byte[] eventTypeBytes
```
With a payload fallback:
```java
String eventType = eventTypeBytes != null
    ? new String(eventTypeBytes, StandardCharsets.UTF_8)
    : (String) payload.getOrDefault("eventType", "UNKNOWN");
```
Unknown event types now log a warning and ACK immediately to unblock the consumer.

#### Bug 3 — Kafka Publish Before DB Commit

`KafkaEventServiceImpl.sendOrderCreated()` was calling `producer.publishOrderEvent(event)` directly, without waiting for the enclosing `@Transactional` method to commit. This created a race condition: payment-service could receive and process `ORDER_INITIATED` before the order row existed in the database, causing a `NullPointerException` when payment-service tried to look up the order.

**Fix:** Wrapped the publish in the existing `syncAndSend()` afterCommit hook:
```java
TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
    @Override
    public void afterCommit() {
        producer.publishOrderEvent(event);
    }
});
```

#### Bug 4 — Idempotency (Bonus Fix)

When Kafka redelivered a `PAYMENT_SUCCESS` event (e.g., after a restart), `updateOrderStatus()` would try to transition an already-PAID order and re-publish an `OrderEvent`, creating duplicate notifications. An idempotency guard was added:

```java
if (order.getStatus() == newStatus) {
    log.info("⏭️ Order {} already has status {} — skipping", id, newStatus);
    return mapper.toResponse(order);
}
```

---

## Phase 12 — Frontend Polish

### OrderCard Button Nesting

React DOM warned: *"button cannot appear as a descendant of button."* The `OrderCard` component wrapped the entire card in a `<button>` for navigation, and the "Pay Now" action inside it was also a `<button>`. This violates the HTML spec.

**Fix:** The outer `<button>` was replaced with:
```jsx
<div
  role="button"
  tabIndex={0}
  onKeyDown={(e) => e.key === "Enter" && navigate(`/orders/${order.id}`)}
  className="... cursor-pointer"
>
```

### My Orders — Stale PENDING Status After Payment

After completing payment and returning to the orders page, the order card still showed PENDING. React Query's default `staleTime` had not expired, so it served the cached (pre-payment) response.

**Fix** in `useOrders.js`:
```js
{
  refetchOnMount: "always",
  staleTime: 0,
  refetchInterval: (query) => {
    const orders = query.state.data;
    return Array.isArray(orders) && orders.some(o => o.orderStatus === "PENDING")
      ? 3000
      : false;
  }
}
```
The query now polls every 3 seconds while any order is in PENDING state, and stops polling once all orders are settled.

### Dummy Reviews

Product reviews showed a blank state when no reviews existed (not just on API error). Changed the guard from:
```js
const usingDummies = reviewsError;
```
to:
```js
const usingDummies = reviewsError || !reviewsRaw?.length;
```
A "(sample reviews)" badge is shown when dummy data is displayed, so users know these are placeholders.

---

## Phase 13 — Admin User Management

### ROLE_MODERATOR Dropdown

The admin users table had a binary role selector (User / Admin). It did not surface the MODERATOR role, and the role detection was using a boolean flag `isAdminUser` which could not represent three states.

**Fix:**
```js
const hasRole = (r) =>
    user.role === r || (Array.isArray(user.roles) && user.roles.includes(r));
const currentRole = hasRole("ROLE_ADMIN") ? "ROLE_ADMIN"
    : hasRole("ROLE_MODERATOR") ? "ROLE_MODERATOR"
    : "ROLE_USER";
```
A third `<SelectItem value="ROLE_MODERATOR">Moderator</SelectItem>` was added between User and Admin. Avatar colours were differentiated: blue for User, amber for Moderator, accent colour for Admin.

---

## Phase 14 — Change Password Feature

### Problem

The profile page had a fully-built `ChangePasswordSection` React component with current/new/confirm fields, show/hide toggle, and a call to `PATCH /api/v1/users/password`. But it was always hidden — the condition was:

```js
const isOAuth = !user?.hasPassword;
```

`user` came from `UserProfileDto`, which at that point had only four fields: `id`, `name`, `email`, `role`. There was no `hasPassword` field. So `user?.hasPassword` was always `undefined`, making `isOAuth` always `true`, and the password change form was never shown — even for email/password accounts.

### Fix — common-contracts

Added `hasPassword` as a 5th field to `UserProfileDto`:
```java
public record UserProfileDto(
    Long id,
    String name,
    String email,
    String role,
    boolean hasPassword
) {}
```

### Fix — user-service

Updated `UserService.getUserProfile()` and `UserService.updateProfile()` to compute and pass `hasPassword`:
```java
boolean hasPassword = user.getPassword() != null && !"{OAUTH}".equals(user.getPassword());
return new UserProfileDto(user.getId(), user.getName(), user.getEmail(),
                          user.getRole().name(), hasPassword);
```

### Fix — UserControllerTest.java

All four `UserProfileDto` mock stubs in `UserControllerTest` used the old 4-arg positional constructor and would not compile after the record gained a 5th field:
```java
// Before:
.thenReturn(new UserProfileDto(1L, "Alice", "alice@example.com", "ROLE_USER"));
// After:
.thenReturn(new UserProfileDto(1L, "Alice", "alice@example.com", "ROLE_USER", true));
```

`UserServiceTest` was not affected — it calls the real service and asserts on returned values rather than constructing the DTO directly.

---

## Architectural Patterns Used

### Saga Pattern (Choreography)

Order creation uses a choreography-based saga without a central coordinator:

1. `order-service` creates the order and publishes `ORDER_INITIATED`
2. `payment-service` consumes it, processes payment, publishes `PAYMENT_SUCCESS` or `PAYMENT_FAILED`
3. `order-service` consumes the payment outcome, updates the order status, publishes the result
4. `notification-service` consumes the final event and sends an email

Compensation (rollback) is handled by publishing `ORDER_CANCELLED` which reverses any inventory reservation.

### CQRS (Command Query Responsibility Segregation)

The review system separates writes and reads:
- **Write side:** `review-rating-service` handles `POST /reviews`, persists to PostgreSQL, publishes events
- **Read side:** `rating-aggregator-service` maintains a pre-computed `RatingAggregate` projection in PostgreSQL, served from Redis cache

### Idempotent Consumers

Every Kafka consumer that mutates state checks whether the state change has already been applied before proceeding. This ensures that Kafka message redelivery (which is guaranteed *at-least-once*) does not cause duplicate effects.

### Transactional Outbox (via afterCommit hook)

All Kafka publishes from services with `@Transactional` methods are wrapped in a `TransactionSynchronization.afterCommit()` callback. This ensures Kafka never receives an event for a database row that failed to commit.

### JWT + HTTP-only Cookie

`user-service` issues JWTs stored as HTTP-only, Secure, SameSite=Strict cookies. The API Gateway validates the JWT on every request and injects the `X-User-Id` and `X-User-Email` headers downstream. Services trust these headers rather than re-validating the token themselves.

---

## Summary of All Bugs Fixed

| # | Service | Bug | Root Cause | Fix |
|---|---------|-----|------------|-----|
| 1 | cart | +/- buttons double-set quantity | Delta vs absolute mismatch | Backend computes `current + delta` |
| 2 | user-service | OAuth 403 | Wrong path in `permitAll()` | Match actual callback path |
| 3 | user-service | Admin users 500 | LazyInitializationException | `@Transactional(readOnly=true)` + DTO projection |
| 4 | payment | my-latest 400 spam | No query deduplication | `staleTime` + `enabled` guard |
| 5 | user-service | Logout doesn't clear session | Cookie not deleted | `Max-Age=0` on logout |
| 6 | order-service | Orders stuck PENDING | Consumer group ID mismatch | Match YAML to `KafkaGroups` constants |
| 7 | order-service | Orders stuck PENDING | `@Header required=true` blocks consumer | `required = false` + payload fallback |
| 8 | order-service | Kafka race condition | Publish before DB commit | `afterCommit` hook via `syncAndSend` |
| 9 | order-service | Duplicate notifications | No idempotency guard | No-op if status already matches |
| 10 | frontend | DOM warning | `<button>` inside `<button>` | Outer to `<div role="button">` |
| 11 | frontend | PENDING not updating | Stale React Query cache | `refetchOnMount: "always"` + polling |
| 12 | review-rating | 302 redirects | Missing `SecurityConfig` | Added proper JWT security chain |
| 13 | rating-aggregator | Model mismatch | Wrong primary key type | Align entity with test expectations |
| 14 | both review services | Import errors | Sub-package scan issue | Flatten package structure |
| 15 | common-contracts | `UserProfileDto` missing field | `hasPassword` not in record | Added 5th field |
| 16 | user-service | Password change always hidden | `hasPassword` always undefined | Compute and pass field in service |
| 17 | user-service tests | Compile error | 4-arg DTO constructor | Add `true` as 5th arg in test stubs |

---

*Document generated: May 2026*
