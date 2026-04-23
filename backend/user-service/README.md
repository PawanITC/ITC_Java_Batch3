# 👤 Funkart User Microservice

The Funkart User Service is the **central identity authority** of the entire system.

It is responsible for:
- Authentication (email/password + OAuth)
- JWT generation + validation
- Security context population
- Role enforcement
- User lifecycle management
- OAuth account linking

---

## 🧠 Architectural Role

This service is the:

> 🟢 SINGLE SOURCE OF TRUTH FOR IDENTITY

All downstream services trust this service for authentication correctness.

---

## 🔐 Authentication Architecture

### Core Flow


### Components

#### 1. JwtWebFilter
- Extracts JWT from header or cookie
- Delegates validation to JwtService
- Delegates identity reconstruction to PrincipalFactory
- Sets Spring SecurityContext

#### 2. JwtService
- Signs JWTs (login/signup/OAuth)
- Validates token signature + expiry
- Extracts claims safely

#### 3. PrincipalFactory
- Converts:
  - User → Principal
  - JWT Claims → Principal
- Enforces:
  - Role whitelist
  - Strict identity validation
  - Claim integrity rules

#### 4. UserContextService
- Fast path: SecurityContext access
- DB rehydration path: enriched user fetch

---

## 🔐 Role System

Supported roles:

- ROLE_USER
- ROLE_MODERATOR
- ROLE_ADMIN

Rules:
- All roles normalized to `ROLE_` prefix
- Strict whitelist enforced in PrincipalFactory
- Invalid roles → JWT rejected

---

## 🔑 OAuth (GitHub)

### Flow:

1. GitHub returns OAuth code
2. GithubOAuthService:
   - exchanges code for access token
   - fetches GitHub profile
   - resolves or creates local user
   - links OAuthAccount
3. JWT issued via AuthFacadeService

---

## 👤 User Lifecycle

### Signup
- validate input
- hash password
- persist user
- publish Kafka signup event
- generate JWT

### Login
- validate credentials
- authenticate user
- publish login event
- generate JWT

---

## 🔄 OAuth Account Linking

Entity: `OAuthAccount`

- user_id + provider unique constraint
- supports multiple providers per user
- idempotent linking via findOrCreate

---

## 📡 Event System (Kafka)

### Events

- UserSignupEvent → synchronous
- UserLoginEvent → asynchronous

### Topics

- `user-signup`
- `user-login`

---

## 🔐 Security Model

- Stateless authentication
- JWT-based identity
- No session storage
- SecurityContext rebuilt per request

---

## ⚠️ Validation Rules

- JWT subject MUST be numeric userId
- Missing claims → rejected
- Invalid roles → rejected
- Tampered tokens → rejected
- malformed subject → rejected early

---

## 🧪 Testing Strategy

- JwtService unit tests
- PrincipalFactory validation tests
- OAuth integration tests
- SecurityContext propagation tests
- Kafka event verification tests

---

## 🛠️ Tech Stack

- Spring Boot 3+
- Spring Security
- JJWT (token handling)
- WebClient (OAuth)
- Kafka (event streaming)
- Hibernate / JPA
- Lombok

---

## 🚀 System Philosophy

> Authentication is centralized, stateless, and strictly validated at the edge of the user-service only.

Gateway is dumb. Services are strict. Identity is authoritative here.
