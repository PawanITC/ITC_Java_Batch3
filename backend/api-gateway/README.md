# 🛡️ Funkart API Gateway

The **Funkart API Gateway** serves as the centralized entry point and "Bouncer" for the Funkart microservices architecture. It is built on **Spring Cloud Gateway (Reactive)** to provide high-performance, non-blocking request routing with a heavy focus on security and identity verification.

## 🌟 Key Features

* **Reactive Security Perimeter:** Utilizes Spring Security WebFlux to manage access control at the edge.
* **JWT Validation:** A custom `JwtWebFilter` intercepts requests to validate JSON Web Tokens issued by the `user-service`.
* **OAuth2 Bridge:** Implements a specialized `GithubOAuthService` to handle the exchange of GitHub codes for system-level identity tokens.
* **Path-Based Routing:** Intelligent routing logic to distinguish between public endpoints and protected resources.
* **Stateless Cookie Management:** Efficient handling of secure, HTTP-only cookies for token transport.

## 🏗️ Architecture Overview

The Gateway acts as a security filter before requests ever reach downstream services. It handles:
1.  **Authentication:** Is the user who they say they are?
2.  **Authorization:** Does the user have permission to access this specific microservice?
3.  **Transformation:** Stripping or adding headers/cookies to maintain a clean internal state.

## 🧪 Quality Assurance & Testing

This project maintains a **100% Code Coverage** standard for all core security logic and utility classes.

![Test Coverage Report](./testing-coverage.jpg)

* **Unit Testing:** Comprehensive suites for `JwtTokenValidator` and `CookieUtil` using JUnit 5.
* **Integration Testing:** `SecurityConfigTest` ensures the filter chain correctly blocks unauthorized traffic and permits public traffic.
* **Reactive Testing:** Extensive use of `StepVerifier` to test non-blocking `Mono` and `Flux` streams in the OAuth service.
* **Semantic Reporting:** All tests utilize `@DisplayName` to provide a human-readable living specification of system behavior.

## 🛠️ Tech Stack

* **Core:** Spring Boot 3.3.5, Spring Cloud Gateway
* **Security:** Spring Security (Reactive), JJWT (Java JWT)
* **Logic:** Project Reactor (Flux/Mono)
* **Testing:** JUnit 5, Mockito, AssertJ, WebTestClient

## 🚦 Getting Started

### Prerequisites
* Java 17 or higher
* Maven or Gradle
* Environment Variables:
    * `JWT_SECRET`: A Base64 encoded 256-bit string.
    * `GITHUB_CLIENT_ID`: Your OAuth App ID.
    * `GITHUB_CLIENT_SECRET`: Your OAuth App Secret.

### Running the Application
```bash
./gradlew bootRun
# or
mvn spring-boot:run