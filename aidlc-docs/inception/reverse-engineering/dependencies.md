# Dependencies

## Internal Dependencies

Single-module project — no inter-package/module dependencies.

```mermaid
flowchart LR
    App["com.ppip.dallyeo<br/>(DallyeoApplication)"] --> SB["Spring Boot Starters"]
```

## External Dependencies

### spring-boot-starter-web
- **Version**: 4.0.6 (BOM-managed)
- **Purpose**: Embedded server, Spring MVC, JSON (Jackson).
- **License**: Apache-2.0
- **Scope**: implementation

### spring-boot-starter-validation
- **Version**: 4.0.6 (BOM-managed)
- **Purpose**: Jakarta Bean Validation.
- **License**: Apache-2.0
- **Scope**: implementation

### spring-boot-starter-data-jpa
- **Version**: 4.0.6 (BOM-managed)
- **Purpose**: JPA/Hibernate ORM, transaction management.
- **License**: Apache-2.0
- **Scope**: implementation

### com.mysql:mysql-connector-j
- **Version**: BOM-managed
- **Purpose**: MySQL JDBC driver.
- **License**: GPL-2.0 with FOSS/Universal exception
- **Scope**: runtimeOnly

### spring-boot-starter-security
- **Version**: 4.0.6 (BOM-managed)
- **Purpose**: Authentication/authorization filter chain.
- **License**: Apache-2.0
- **Scope**: implementation

### spring-boot-starter-data-redis
- **Version**: 4.0.6 (BOM-managed)
- **Purpose**: Redis client (Lettuce), cache/token store.
- **License**: Apache-2.0
- **Scope**: implementation

### org.projectlombok:lombok
- **Version**: BOM-managed
- **Purpose**: Compile-time boilerplate generation.
- **License**: MIT
- **Scope**: compileOnly + annotationProcessor (and test equivalents)

## Test Dependencies
- `spring-boot-starter-data-jpa-test`, `spring-boot-starter-data-redis-test`, `spring-boot-starter-jdbc-test`, `spring-boot-starter-security-test`, `spring-boot-starter-webmvc-test` — sliced test support (Apache-2.0, testImplementation).
- `org.junit.platform:junit-platform-launcher` — testRuntimeOnly.

## Observations / Gaps
- **No JWT library declared** despite JWT configuration in `application.properties`. A library (e.g. `io.jsonwebtoken:jjwt` or `com.nimbusds:nimbus-jose-jwt`) must be added when authentication is implemented.
- Test starter names use a non-standard `-test` suffix pattern (e.g. `spring-boot-starter-webmvc-test`) rather than the conventional `spring-boot-starter-test` / `spring-boot-starter-data-jpa` + `@DataJpaTest`. Should be verified/resolved against the Spring Boot 4.x BOM during Build & Test.
