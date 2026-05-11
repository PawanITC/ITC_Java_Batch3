# user-service

Single-responsibility service owning all identity, authentication, and account management for the Funkart platform.

## Responsibilities

- Email/password signup and login with BCrypt password hashing
- JWT issuance (HS256, HTTP-only cookie, 24-hour expiry)
- GitHub OAuth2 login — creates or retrieves a local user on first sign-in
- User profile reads and updates (name, password change)
- Admin-only user management (list all users, change role, toggle active status)
- Publishes login and signup events to Kafka for downstream audit/notification

---

## REST Endpoints

All routes are prefixed by the API Gateway at `/api/v1`.

### Auth (`/users`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/users/signup` | Public | Register a new email/password account. Validates email uniqueness, hashes password, returns 201 with JWT cookie set. |
| POST | `/users/login` | Public | Authenticate with email + password. Validates credentials, issues JWT cookie. |
| POST | `/users/oauth/github` | Public | OAuth2 callback handler. Exchanges GitHub code for profile, calls `getOrCreateOAuthUser`, issues JWT cookie. |
| GET | `/users/me` | Authenticated | Returns `UserProfileDto` — id, name, email, role, `hasPassword` flag used by frontend to show/hide the change-password form. |
| PATCH | `/users/profile` | Authenticated | Updates display name. Rejects blank names. |
| PATCH | `/users/password` | Authenticated | Changes password after verifying current. Blocked for OAuth-only accounts (password stored as `{OAUTH}` sentinel). |

### Admin (`/admin/users`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/admin/users` | ADMIN | Returns all registered users as a flat DTO list (no JPA lazy-load exposure). |
| PATCH | `/admin/users/{userId}/role` | ADMIN | Changes a user's role to `ROLE_USER`, `ROLE_MODERATOR`, or `ROLE_ADMIN`. Self-demotion guard prevents an admin removing their own privileges. |
| PATCH | `/admin/users/{userId}/status` | ADMIN | Toggles `isActive`. Blocks deactivating your own account. |

---

## How Each Feature Works

### Signup
1. Validate email format and non-blank name + password.
2. Check `userRepository.findByEmail` — throw `AlreadyExistsException` (409) if taken.
3. BCrypt-hash the password, persist `User` entity with `ROLE_USER`.
4. Publish `USER_REGISTERED` event to `user.events.v1` via Kafka (for notification-service).
5. Generate JWT with claims `{sub: userId, email, role}`, set as HTTP-only `Secure SameSite=Strict` cookie.

### Login
1. Fetch user by email — throw `UnauthorizedException` (401) if not found.
2. `passwordEncoder.matches(raw, hashed)` — throw 401 if wrong.
3. Publish `USER_LOGGED_IN` event.
4. Issue JWT cookie identical to signup flow.

### GitHub OAuth2
1. Spring Security's OAuth2 client handles the GitHub redirect and token exchange.
2. `OAuth2LoginSuccessHandler` extracts `email` and `name` from the GitHub principal.
3. `UserService.getOrCreateOAuthUser(email, name)` is **idempotent** — first login creates the row with password `{OAUTH}`; subsequent logins return the existing user.
4. JWT cookie issued and browser redirected to the frontend dashboard.

### JWT Validation
The API Gateway reads the cookie on every inbound request, validates the signature and expiry, and injects `X-User-Id` and `X-User-Email` headers. Downstream services trust these headers; they do not re-validate the token.

### Change Password
1. Reject if `password == "{OAUTH}"` — OAuth accounts have no local credential.
2. `passwordEncoder.matches(currentPassword, stored)` — throw `UnauthorizedException` on mismatch.
3. Validate new password length ≥ 8.
4. Hash and save.

### Profile DTO — `hasPassword` flag
`UserProfileDto` carries a boolean `hasPassword` computed as `password != null && !"{OAUTH}".equals(password)`. The frontend uses this to conditionally render the change-password section on the profile page.

---

## Kafka

| Topic | Direction | Event | Trigger |
|-------|-----------|-------|---------|
| `user.events.v1` | Producer | `USER_REGISTERED` | After successful signup |
| `user.events.v1` | Producer | `USER_LOGGED_IN` | After successful login (any method) |

---

## Storage

**PostgreSQL** — `users` table

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| name | VARCHAR | Display name |
| email | VARCHAR UNIQUE | |
| password | VARCHAR | BCrypt hash or `{OAUTH}` |
| role | ENUM | `ROLE_USER`, `ROLE_MODERATOR`, `ROLE_ADMIN` |
| is_active | BOOLEAN | Defaults true; toggled by admin |

Schema managed by **Flyway** migrations (`V1__init_schema_and_admin.sql`, `V2__set_admin_password.sql`, `V3__add_active_status.sql`).

---

## Security

- Spring Security filter chain: JWT extraction → `UsernamePasswordAuthenticationToken` → `SecurityContext`.
- `@PreAuthorize("hasRole('ADMIN')")` guards all `/admin/**` routes.
- Logout: `POST /users/logout` responds with `Set-Cookie: jwt=; Max-Age=0` to immediately expire the cookie.
- CORS configured for the frontend origin only.

---

## Observability

- **OTEL**: HTTP spans + Kafka producer spans exported to OTLP collector on port 4318 (HTTP).
- **Logs**: MDC pattern `[service-name, traceId, spanId]` on every log line.
- **Metrics**: Micrometer → Prometheus scrape at `/actuator/metrics`.
