# product-service

Owns the product catalogue, category taxonomy, shopping cart, and the checkout event that kicks off the order saga.

## Responsibilities

- Browse and search products with pagination and filtering
- Admin CRUD for products and categories
- Per-user shopping cart (add, update quantity, remove)
- Cart checkout: validates stock, publishes `CHECKOUT_INITIATED` to Kafka to hand off to order-service
- Batch product lookup endpoint used internally by order-service

---

## REST Endpoints

All routes prefixed at `/api/v1` by the API Gateway.

### Public catalogue

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/products` | Public | Paginated product list. Query params: `page`, `size`, `search` (name/description), `category`, `minPrice`, `maxPrice`, `sort`. |
| GET | `/products/{id}` | Public | Single product detail including images, price, stock. |
| POST | `/products/batch` | Internal | Accepts a list of product IDs, returns a list of `ProductDto`. Used by order-service to enrich order line items. |
| GET | `/categories` | Public | All categories. |
| GET | `/categories/{id}` | Public | Single category. |

### Cart (`/cart`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/cart/my-cart` | Authenticated | Returns the caller's current cart with line items and computed total. |
| POST | `/cart/items` | Authenticated | Add a product to cart. Creates cart if none exists. Validates `productId` and `quantity > 0`. |
| PATCH | `/cart/items/{productId}` | Authenticated | Update quantity by **delta** (e.g. `+1`, `-1`). If resulting quantity ≤ 0 the item is removed. This was a deliberate fix — the original implementation applied the incoming value as an absolute quantity. |
| DELETE | `/cart/items/{productId}` | Authenticated | Remove a specific item. |
| POST | `/cart/checkout` | Authenticated | Validates stock for each item, clears the cart, publishes `CHECKOUT_INITIATED` event to Kafka, and returns the event payload so the frontend knows a checkout is in flight. |

### Admin products (`/admin/products`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/admin/products` | ADMIN | Create product. Accepts name, description, price (cents), stock, category, and image URL list. |
| PUT | `/admin/products/{id}` | ADMIN | Full update of a product record. |
| DELETE | `/admin/products/{id}` | ADMIN | Soft-deletes the product (sets `isActive = false`). |

### Admin categories (`/admin/categories`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/admin/categories` | ADMIN | Create a category. |
| DELETE | `/admin/categories/{id}` | ADMIN | Delete a category. |

---

## How Each Feature Works

### Product listing and search
The `GET /products` handler calls `ProductService.findAll(PageRequest, filters)` which builds a JPA `Specification` from the optional query parameters and passes it to `ProductRepository.findAll(spec, pageable)`. The response is a `Page<ProductDto>` — Spring's pagination wrapper serialised to JSON with `content`, `totalElements`, `totalPages`, and `number`.

### Cart — delta quantity fix
The frontend sends `{ delta: +1 }` or `{ delta: -1 }`. `CartService.updateQuantity(userId, productId, delta)` fetches the existing `CartItem`, computes `newQty = current + delta`, and removes the row if `newQty <= 0`. The original scaffold treated the incoming value as the absolute new quantity, causing the cart counter to stick at 1.

### Checkout flow
1. `POST /cart/checkout` → `CartService.checkout(userId)` fetches all cart items.
2. Validates each item has sufficient stock (`product.stock >= item.quantity`). Throws 409 if any item is out of stock.
3. Deducts stock for each item (`product.stock -= qty`) — **within a single `@Transactional` call** to avoid overselling.
4. Clears the cart.
5. Builds a `CheckoutInitiatedEvent` (userId, items list, total amount, timestamp).
6. Publishes to `checkout.events.v1`. order-service consumes this and creates the `Order` row.
7. Returns the event payload to the frontend. The frontend uses it to start polling `GET /orders/history` for the new order.

### Batch product lookup
`POST /products/batch` accepts `{ ids: [1, 2, 3] }` and returns a list of `ProductDto`. order-service calls this to enrich order line items with current names and images without needing direct DB access across service boundaries.

---

## Kafka

| Topic | Direction | Event | Trigger |
|-------|-----------|-------|---------|
| `checkout.events.v1` | Producer | `CheckoutInitiatedEvent` | `POST /cart/checkout` success |

---

## Storage

**PostgreSQL** — shared `mydb` database

| Table | Key columns | Notes |
|-------|------------|-------|
| `products` | id, name, description, price, stock, is_active, category_id | Soft-deleted via `is_active` |
| `categories` | id, name, description | |
| `cart_items` | id, user_id, product_id, quantity | One row per user+product combination |

**Redis** — used by the API Gateway for rate limiting. product-service itself reads Redis for session context injected by the gateway.

---

## Security

- Public read endpoints (`/products/**`, `/categories/**`) require no token.
- Cart and checkout require a valid JWT (user extracted from `X-User-Id` gateway header).
- Admin endpoints require `ROLE_ADMIN` via `@PreAuthorize`.

---

## Observability

- OTEL traces exported via Micrometer OTLP HTTP to port 4318.
- The `dev` Spring profile (`SPRING_PROFILES_ACTIVE=dev` in `.env`) disables tracing locally. In Docker this is overridden via `JAVA_TOOL_OPTIONS=-Dmanagement.tracing.enabled=true` in `docker-compose.yml`.
