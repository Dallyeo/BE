# System Architecture

## System Overview

Dallyeo is a single-module Spring Boot 4.0.6 monolith running on Java 17, built with Gradle. At present the application is a bootstrap-only scaffold: it starts a Spring context and an embedded web server on port 8080, with datasource, JPA, Redis, Security, and JWT settings pre-configured but no domain, web, service, or persistence code implemented yet.

## Architecture Diagram

```mermaid
flowchart TD
    Client(["HTTP Client"])

    subgraph App["Dallyeo Spring Boot App (port 8080)"]
        direction TB
        Boot["DallyeoApplication<br/>@SpringBootApplication"]
        Web["spring-boot-starter-web<br/>(MVC / REST) — configured"]
        Sec["spring-boot-starter-security<br/>+ JWT — configured"]
        JPA["spring-boot-starter-data-jpa<br/>(Hibernate) — configured"]
        RedisC["spring-boot-starter-data-redis — configured"]
    end

    MySQL[("MySQL 3306<br/>db: dallyeo")]
    Redis[("Redis 6379")]

    Client --> Web
    Web --> Sec
    Web --> JPA
    Sec --> RedisC
    JPA --> MySQL
    RedisC --> Redis
```

## Component Descriptions

### DallyeoApplication
- **Purpose**: Composition root and process entry point.
- **Responsibilities**: Boots the Spring context; enables auto-configuration for all starters on the classpath.
- **Dependencies**: Spring Boot autoconfiguration.
- **Type**: Application.

### Configured (not-yet-implemented) layers
- **Web/REST layer**: Provided by `spring-boot-starter-web` + `spring-boot-starter-validation`. No controllers exist yet.
- **Security layer**: `spring-boot-starter-security` on the classpath (default security filter chain currently active). JWT properties are configured but no filter/provider code exists.
- **Persistence layer**: `spring-boot-starter-data-jpa` + MySQL connector. `ddl-auto=update`. No entities or repositories exist yet.
- **Cache/Token layer**: `spring-boot-starter-data-redis`. No Redis templates or repositories exist yet.

## Data Flow

No implemented data flows yet. Intended (inferred) authentication flow once implemented:

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Security Filter (JWT)
    participant Svc as Service Layer
    participant DB as MySQL
    participant R as Redis
    C->>S: Request with Bearer token
    S->>S: Validate JWT (access token)
    S->>Svc: Authorized request
    Svc->>DB: Read/write domain data
    Svc->>R: Read/write cached / refresh-token data
    Svc-->>C: Response
```

## Integration Points
- **External APIs**: None configured.
- **Databases**: MySQL (`jdbc:mysql://localhost:3306/dallyeo`, timezone Asia/Seoul, UTF-8).
- **Third-party Services**: None.
- **Cache/Store**: Redis (`localhost:6379`).

## Infrastructure Components
- **CDK Stacks**: None.
- **Deployment Model**: Not defined (local dev configuration only; embedded server on port 8080).
- **Networking**: Not defined.
- **Configuration**: `application.properties` + included `secret` profile (`application-secret.properties`, git-ignored). Secrets injected via env vars: `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`.
