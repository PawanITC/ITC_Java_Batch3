# review-rating-service & rating-aggregator-service

A CQRS pair. `review-rating-service` is the write side — it accepts review submissions, enforces one-review-per-user-per-product, and publishes events to Kafka. `rating-aggregator-service` is the read side — it consumes those events, maintains a pre-computed per-product `RatingSummary` row, and serves it via REST. The two services never share a database; they communicate exclusively through `review.events.v1`.

---

## review-rating-service (Write Side)

### Responsibilities

- Accept new reviews from authenticated users
- Enforce one review per user per product (unique constraint at the application layer)
- Store review rows with author snapshot (name taken from JWT at submission time)
- Allow moderators/admins to hard-delete any review
- Publish `REVIEW_CREATED` and `REVIEW_DELETED` events to Kafka so the aggregator stays consistent

---

### REST Endpoints

All routes prefixed at `/api/v1` by the API Gateway.

#### Reviews (`/reviews`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/reviews/{productId}` | Public | Paginated list of reviews for a product, newest first. Params: `page` (default 0), `size` (default 20). |
| POST | `/reviews/{productId}` | Authenticated | Submit a review. Body: `title`, `comment`, `rating` (1–5). Throws 403 if the user has already reviewed this product. |
| DELETE | `/reviews/admin/{reviewId}` | MODERATOR / ADMIN | Hard-delete a review. No ownership check — any moderator can delete any review. |

---

### How Each Feature Works

#### Review submission
1. `POST /reviews/{productId}` extracts `UserPrincipalDto` from the `Authentication` object (populated by the gateway's `X-User-Id` / `X-User-Email` headers via `JwtAuthFilter`).
2. `ReviewService.createReview()` calls `reviewRepository.existsByProductIdAndUserId(productId, userId)` — throws `ForbiddenException` (403) if a row already exists for this pair.
3. A `Review` entity is built with `author` snapshotted from the JWT's `name` claim. Author name is never updated if the user later changes their display name — the review records the name at submission time.
4. The entity is persisted, then `ReviewEventPublisher.publishReviewCreated()` publishes a `REVIEW_CREATED` event to `review.events.v1` with the review ID, product ID, user ID, and rating value.
5. The publish is fire-and-forget with a `whenComplete` callback for error logging. If Kafka is unavailable the review is still saved (no outbox / transactional publish here).

#### Moderator deletion
1. `DELETE /reviews/admin/{reviewId}` is restricted to `ROLE_MODERATOR` or `ROLE_ADMIN` at the `SecurityConfig` level.
2. `ReviewService.deleteReview()` fetches the review, hard-deletes it, then calls `ReviewEventPublisher.publishReviewDeleted()` with the original rating value so the aggregator can reverse the rating contribution.

#### Author snapshot
The `author` column is a plain `VARCHAR` copied from the JWT claim at creation time. This means reviews remain attributable even if the user deletes their account or changes their display name. There is no foreign key to the users table — review-rating-service has no knowledge of user-service.

---

### Kafka

| Topic | Direction | Event | Trigger |
|-------|-----------|-------|---------|
| `review.events.v1` | Producer | `REVIEW_CREATED` | Review submitted successfully |
| `review.events.v1` | Producer | `REVIEW_DELETED` | Moderator deletes a review |

Message key: `productId` (as string). This ensures all events for the same product land on the same partition, so the aggregator processes `REVIEW_CREATED` and `REVIEW_DELETED` for a product in order.

---

### Storage

**PostgreSQL** — `mydb` (shared database)

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| product_id | BIGINT | Indexed |
| user_id | BIGINT | Indexed |
| author | VARCHAR | Display name snapshot from JWT at write time |
| title | VARCHAR(200) | Optional short heading |
| comment | TEXT | Required review body |
| rating | INT | 1–5 stars |
| likes | INT | Defaults 0 (like feature not yet wired in frontend) |
| created_at | TIMESTAMP | Set by `@PrePersist`; immutable after creation |

Unique constraint enforced in application code via `existsByProductIdAndUserId` (no DB-level unique index — a race could allow duplicates but is acceptable for MVP).

---

### Security

- Public read (`GET /reviews/{productId}`) — no token required.
- Write (`POST /reviews/{productId}`) — requires valid JWT.
- Admin delete (`DELETE /reviews/admin/{reviewId}`) — requires `ROLE_MODERATOR` or `ROLE_ADMIN` enforced at `SecurityConfig`.
- `JwtAuthFilter` validates the cookie, extracts `userId`, `email`, `name`, and `role`, and wires a `UsernamePasswordAuthenticationToken` into the `SecurityContext`. Downstream services do not re-validate.

---

---

## rating-aggregator-service (Read Side)

### Responsibilities

- Consume `REVIEW_CREATED` and `REVIEW_DELETED` events from Kafka
- Maintain a single pre-computed `RatingSummary` row per product with total count, sum, average, and per-star buckets
- Serve the summary via REST — public, no auth required
- Return a zeroed/empty summary for products that have no reviews yet (no 404)

---

### REST Endpoints

All routes prefixed at `/api/v1` by the API Gateway.

#### Rating summary (`/rating-summary`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/rating-summary/{productId}` | Public | Returns the pre-computed rating summary for a product. Returns a zeroed summary if no reviews exist. |

---

### How Each Feature Works

#### Kafka consumption
`ReviewEventConsumer` listens on `review.events.v1`. Events are deserialised as `Map<String, Object>` using `ErrorHandlingDeserializer` wrapping `JsonDeserializer` — bad/malformed events are logged and dropped without crashing the consumer, and the offset always advances.

For each well-formed event:
- `REVIEW_CREATED` → `handleCreated(productId, rating)` — fetches or creates a `RatingSummary` row, increments `totalReviews`, adds the rating to `sumRatings`, increments the appropriate star bucket, and recomputes `averageRating = sumRatings / totalReviews`.
- `REVIEW_DELETED` → `handleDeleted(productId, rating)` — decrements `totalReviews` and `sumRatings`, decrements the star bucket (floored at 0), and recomputes the average. If the product has no summary row the deletion is a no-op.

Both handlers use `Math.max(0, ...)` to prevent negative counts from duplicate-delivery or out-of-order events.

#### Summary serving
`GET /rating-summary/{productId}` delegates to `RatingSummaryService.getSummary()` which does a direct `findById(productId)` on the `RatingSummary` table. If no row exists (product has never been reviewed), it returns `RatingSummaryResponse.empty(productId)` — a zeroed object — rather than a 404. This keeps the frontend product page simple: it can always render the star display without a null check.

#### CQRS separation
The aggregator has its own `Review` entity table (a read-side projection) but the primary read path goes through `RatingSummary`, not raw reviews. The raw review list is served by review-rating-service; the aggregator only answers "what is this product's star rating?".

---

### Kafka

| Topic | Direction | Events consumed | Action |
|-------|-----------|----------------|--------|
| `review.events.v1` | Consumer | `REVIEW_CREATED`, `REVIEW_DELETED` | Update `RatingSummary` row for the affected product |

**Consumer group**: `rating-aggregator-service-group-v1`  
**auto-offset-reset**: `earliest`  
**deserialiser**: `ErrorHandlingDeserializer` → `JsonDeserializer` → `Map<String, Object>` (no type headers, no class mapping needed — the service only reads `eventType`, `productId`, and `rating`).

---

### Storage

**PostgreSQL** — `mydb` (shared database)

`rating_summary` table — one row per product, `productId` is the primary key.

| Column | Type | Notes |
|--------|------|-------|
| product_id | BIGINT PK | Maps to product-service product ID |
| total_reviews | INT | Running count |
| sum_ratings | INT | Running sum of all star values |
| average_rating | DOUBLE | `sum_ratings / total_reviews`; 0.0 when total is 0 |
| one_star | INT | Count of 1-star reviews |
| two_star | INT | Count of 2-star reviews |
| three_star | INT | Count of 3-star reviews |
| four_star | INT | Count of 4-star reviews |
| five_star | INT | Count of 5-star reviews |

Schema managed by `ddl-auto: update` (dev/Docker). No Redis cache is wired in the current implementation despite the Redis dependency being declared — the `application.yaml` configures a Redis connection but no `@Cacheable` annotations are applied. Redis is available if caching is added later.

---

### Security

All endpoints on the aggregator are public — read-only aggregate data requires no authentication. `SecurityConfig` permits all requests. The `JwtAuthFilter` bean is present (for potential future protected writes) but is not applied to any route.

---

### Observability

Both services export OTEL traces via Micrometer OTLP HTTP to the collector on port 4318. Kafka consumer spans are linked to the producer trace via the `traceparent` header, so a review submission in review-rating-service appears as the root span with the aggregator update as a linked child in Jaeger.
