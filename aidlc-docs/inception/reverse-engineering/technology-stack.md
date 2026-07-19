# Technology Stack

## Programming Languages
- **Java** — 17 (Gradle toolchain) — all application and test code.

## Frameworks
- **Spring Boot** — 4.0.6 — application framework / auto-configuration.
- **Spring Web (MVC)** — via `spring-boot-starter-web` — REST/web layer (configured).
- **Spring Validation** — via `spring-boot-starter-validation` — Bean Validation (Jakarta).
- **Spring Data JPA (Hibernate)** — via `spring-boot-starter-data-jpa` — ORM/persistence.
- **Spring Security** — via `spring-boot-starter-security` — authentication/authorization.
- **Spring Data Redis** — via `spring-boot-starter-data-redis` — cache / token store.
- **Lombok** — boilerplate reduction (compile-time).

## Infrastructure
- **MySQL** — relational database (`localhost:3306/dallyeo`).
- **Redis** — in-memory cache / token store (`localhost:6379`).
- **JWT** — token-based auth (access + refresh; secret via env). Note: no JWT library (e.g. jjwt / nimbus) is declared in `build.gradle` yet — will need to be added when JWT is implemented.

## Build Tools
- **Gradle** — Groovy DSL, wrapper-managed.
- **Spring Boot Gradle Plugin** — 4.0.6.
- **io.spring.dependency-management** — 1.1.7 — BOM-based version alignment.

## Testing Tools
- **JUnit 5 (JUnit Platform)** — `useJUnitPlatform()`; `junit-platform-launcher` runtime.
- **Spring Boot Test slices** — data-jpa-test, data-redis-test, jdbc-test, security-test, webmvc-test (declared as `testImplementation`).
