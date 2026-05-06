# FunkArt — React Frontend

A professional e-commerce storefront for FunkArt, built with **Vite + React + Tailwind CSS**.
Connects to a **Spring Boot backend** via HttpOnly JWT cookie authentication.

---

## Tech Stack

| Layer       | Technology                              |
|-------------|------------------------------------------|
| Framework   | React 18 + Vite                         |
| Styling     | Tailwind CSS + shadcn/ui                |
| State       | TanStack React Query v5                 |
| Payments    | Stripe (stripe-js + react-stripe-js)    |
| Auth        | HttpOnly JWT cookie (Spring Boot)       |
| Icons       | Lucide React                            |

---

## Getting Started

### 1. Prerequisites

- Node.js 18+ and npm
- Your Spring Boot backend running (default: `http://localhost:8080`)

### 2. Install dependencies

```bash
npm install
```

### 3. Configure environment

```bash
cp .env.example .env
```

Edit `.env`:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_STRIPE_PUBLISHABLE_KEY=pk_test_YOUR_KEY
```

### 4. Configure Vite proxy (for local dev)

In `vite.config.js`, ensure the proxy is set so `/api/*` forwards to your backend:

```js
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
```

This avoids CORS issues in development. In production, configure your reverse proxy (nginx/caddy) to route `/api/*` to your backend.

### 5. Run the dev server

```bash
npm run dev
```

---

## Project Structure

```
src/
├── api/                  # Base44 SDK client (kept for platform compat)
├── components/
│   ├── cart/             # CartDrawer, CartIcon, CartItem
│   ├── checkout/         # StripeCardForm, OrderSummary
│   ├── layout/           # Header, Layout
│   ├── orders/           # OrderCard, OrderStatusBadge
│   └── admin/            # StatusSelect
├── context/
│   └── CartContext.jsx   # Global cart state
├── hooks/
│   ├── useCurrentUser.js # Fetches /api/v1/auth/me
│   ├── useOrders.js
│   ├── useAdminOrders.js
│   └── useAdminUsers.js
├── lib/
│   ├── api.js            # Base fetch client (cookie-based auth)
│   ├── authApi.js        # Auth endpoints
│   ├── cartApi.js        # Cart endpoints
│   ├── orderApi.js       # Order endpoints
│   ├── productApi.js     # Product & category endpoints
│   ├── paymentApi.js     # Stripe payment intent endpoints
│   └── adminApi.js       # Admin endpoints
├── pages/
│   ├── ProductsPage.jsx  # Browse & filter catalog
│   ├── CartPage.jsx      # Cart management
│   ├── CheckoutPage.jsx  # Stripe checkout
│   ├── PaymentSuccess.jsx
│   ├── PaymentFailure.jsx
│   ├── OrderHistory.jsx
│   ├── OrderDetail.jsx
│   └── admin/
│       ├── AdminDashboard.jsx
│       ├── AdminOrders.jsx
│       ├── AdminUsers.jsx
│       └── AdminCategories.jsx
└── App.jsx               # Router + providers
```

---

## API Endpoints Expected (Spring Boot)

| Method | Path                              | Description               |
|--------|-----------------------------------|---------------------------|
| GET    | `/api/v1/auth/me`                 | Current user profile      |
| POST   | `/api/v1/auth/logout`             | Logout (clears cookie)    |
| GET    | `/api/v1/products`                | List products (paginated) |
| GET    | `/api/v1/categories`              | List categories           |
| GET    | `/api/v1/cart/my-cart`            | Get user's cart           |
| POST   | `/api/v1/cart/items`              | Add item to cart          |
| PATCH  | `/api/v1/cart/items/{productId}`  | Update item quantity      |
| DELETE | `/api/v1/cart/items/{productId}`  | Remove item               |
| POST   | `/api/v1/cart/checkout`           | Create order + intent     |
| GET    | `/api/v1/orders/history`          | User's order history      |
| GET    | `/api/v1/orders/{id}`             | Order detail              |
| PATCH  | `/api/v1/orders/{id}/cancel`      | Cancel order              |
| GET    | `/api/v1/payments/my-latest`      | Latest payment intent     |
| GET    | `/api/v1/admin/orders`            | All orders (admin)        |
| PATCH  | `/api/v1/admin/orders/{id}/status`| Update order status       |
| GET    | `/api/v1/admin/users`             | All users (admin)         |
| PATCH  | `/api/v1/admin/users/{id}/role`   | Update user role          |
| POST   | `/api/v1/admin/categories`        | Create category           |
| DELETE | `/api/v1/admin/categories/{id}`   | Delete category           |

---

## Production Build

```bash
npm run build
```

Output goes to `dist/`. Serve it with nginx, Vercel, Netlify, or any static host.
Point `/api/*` to your Spring Boot server via reverse proxy.

---

## Notes

- Authentication is **cookie-based** (`credentials: "include"` on all requests). The Spring Boot backend must set `SameSite=Lax` / `SameSite=None; Secure` cookies appropriately.
- On a `401` response, the frontend automatically redirects to `/login`.
- Admin links only appear for users with `ROLE_ADMIN` in their roles array.