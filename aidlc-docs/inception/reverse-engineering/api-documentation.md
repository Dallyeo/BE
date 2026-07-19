# API Documentation

## REST APIs

No REST endpoints are implemented yet. `spring-boot-starter-web` is on the classpath, but no `@RestController` / `@Controller` classes exist.

Because Spring Security is on the classpath with no custom `SecurityFilterChain`, the application currently applies Spring Boot's **default security** (all requests require authentication with a generated password) — this is default behavior, not an implemented API surface.

## Internal APIs

### DallyeoApplication
- **Methods**: `public static void main(String[] args)`
- **Parameters**: `args` — standard JVM program arguments passed to `SpringApplication.run`.
- **Return Types**: `void`.

## Data Models

No JPA entities, DTOs, or domain models are defined yet. Persistence is configured (`spring.jpa.hibernate.ddl-auto=update`) but there are no `@Entity` classes for Hibernate to manage.

Configured authentication-related values (not yet backed by models/code):
- **Access Token TTL**: `jwt.access-expiration=3600000` ms (1 hour).
- **Refresh Token TTL**: `jwt.refresh-expiration=604800000` ms (7 days).
- **JWT Secret**: `jwt.secret=${JWT_SECRET}` (env-injected).

> To be defined during Requirements Analysis / Application Design.
