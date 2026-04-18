# 🛡️ Funkart API Gateway

The Funkart API Gateway is the **edge routing layer** of the system and acts strictly as a **network + security perimeter**, not an identity authority.

It is built on **Spring Cloud Gateway (Reactive)** and follows a **stateless pass-through security model**.

---

## 🌟 Core Responsibility

The gateway is responsible for:

- Request routing to downstream microservices
- Forwarding authentication tokens (JWT)
- Optional structural validation of tokens (format-level only)
- Header normalization (Authorization propagation)
- Enforcing public vs protected route access rules

---

## ❌ Explicit Non-Responsibilities (Important)

The gateway MUST NOT:

- ❌ Generate JWTs
- ❌ Decode JWT claims for business logic
- ❌ Perform role-based authorization
- ❌ Build `Authentication` objects
- ❌ Access user database
- ❌ Interpret identity beyond transport

> Identity authority lives ONLY in `user-service`.

---

## 🧭 Architecture Role

The gateway acts as:

**Client → Gateway → User-Service / Other Services**

It is a **dumb router with security awareness**, not a security brain.

---

## 🔐 Security Model

### Current Design (Corrected)

- JWT is issued by `user-service`
- Gateway only forwards token downstream
- Downstream services validate and reconstruct identity

### Token Flow

1. Client sends request with JWT
2. Gateway:
   - extracts Authorization header
   - optionally checks token structure (NOT claims logic)
   - forwards request unchanged
3. User-service:
   - fully validates token
   - builds `SecurityContext`

---

## 🚦 Routing Strategy

- `/auth/**` → User-service
- `/users/**` → User-service
- `/oauth/**` → User-service
- `/products/**` → Product-service (future)

---

## 🧪 Testing Strategy

- Route validation tests
- Security filter chain tests
- Header propagation tests
- Reactive WebTestClient integration tests



## 🧪 Quality Assurance & Testing

This project maintains a **100% Code Coverage** standard for all core security logic and utility classes.

![Test Coverage Report](./testing-coverage.jpg)

* **Unit Testing:** Comprehensive suites for `JwtTokenValidator` and `CookieUtil` using JUnit 5.
* **Integration Testing:** `SecurityConfigTest` ensures the filter chain correctly blocks unauthorized traffic and permits public traffic.
* **Reactive Testing:** Extensive use of `StepVerifier` to test non-blocking `Mono` and `Flux` streams in the OAuth service.
* **Semantic Reporting:** All tests utilize `@DisplayName` to provide a human-readable living specification of system behavior.

---

## 🛠️ Tech Stack

- Spring Boot 3.3+
- Spring Cloud Gateway (WebFlux)
- Spring Security (Reactive filter chain)
- Project Reactor (Mono/Flux)
- JUnit 5 + WebTestClient

---

## 🚨 Key Design Rule

> The gateway is NOT allowed to evolve into a mini-auth service.

All identity logic belongs in `user-service`.

## 🚦 Getting Started

### Prerequisites
* Java 17 or higher
* Gradle
* Environment Variables:
    * `JWT_SECRET`: A Base64 encoded 256-bit string.
    * `GITHUB_CLIENT_ID`: Your OAuth App ID.
    * `GITHUB_CLIENT_SECRET`: Your OAuth App Secret.

### Running the Application
```bash
./gradlew bootRun
