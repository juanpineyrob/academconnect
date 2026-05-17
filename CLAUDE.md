# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Start local PostgreSQL (required for `dev` profile)
docker compose up -d

# Run (defaults to `dev` profile)
./mvnw spring-boot:run

# Build (without tests)
./mvnw clean package -DskipTests

# Tests — require a Docker daemon; Testcontainers boots an ephemeral postgres:16-alpine
./mvnw test
./mvnw test -Dtest=AcademconnectApplicationTests
./mvnw test -Dtest=AcademconnectApplicationTests#contextLoads
```

Datasource is read from env vars `DB_URL`, `DB_USER`, `DB_PASSWORD` (defaults match `docker-compose.yml`).
Active profile: `SPRING_PROFILES_ACTIVE` (default `dev`).

## Tech Stack

- **Java 25**, **Spring Boot 4.0.6** (manages Testcontainers 2.0.5).
- **Spring Web MVC**, **Spring Data JPA**, **Spring Validation**.
- **PostgreSQL 16** (runtime + Testcontainers in tests).
- **Flyway** (`flyway-core` + `flyway-database-postgresql`) — migrations in `src/main/resources/db/migration/`.
- **MapStruct 1.6.3** + Lombok (via `lombok-mapstruct-binding`) — DTO ↔ entity mappers.
- **Testcontainers 2.0.5** — note the renamed artifacts: `testcontainers-junit-jupiter`, `testcontainers-postgresql` (Testcontainers 2.x dropped the bare names).

## Architecture

Layered: `controller` → `service` → `repository` (Spring Data JPA) → PostgreSQL. DTOs in `dto/`, mappers in `mapper/`, domain entities in `domain/`.

`exception.GlobalExceptionHandler` returns RFC 7807 `ProblemDetail` responses with `urn:academconnect:error:*` type URIs. Add new handlers there, not in controllers.

## Testing

Tests use a single shared `PostgreSQLContainer` declared as a `@Bean @ServiceConnection` in `TestcontainersConfiguration` — import it with `@Import(TestcontainersConfiguration.class)` and Spring Boot wires the datasource automatically. Active profile in tests is `test`.

Do not switch to H2 — production uses PostgreSQL-specific features (JSONB, `tsvector`, extensions) that H2 doesn't replicate faithfully.

## Schema migrations

All schema changes go through Flyway (`spring.jpa.hibernate.ddl-auto=validate` enforces this). Add a new `Vn__description.sql` file under `src/main/resources/db/migration/` — never use `update`/`create-drop`.

## Package layout

```
com.academconnect
├── AcademconnectApplication
├── config       # @Configuration beans, security, properties
├── controller   # @RestController only — no business logic
├── domain       # JPA @Entity classes
├── dto          # request/response records, validated with jakarta.validation
├── exception    # ResourceNotFoundException, BusinessException, GlobalExceptionHandler
├── mapper       # @Mapper(componentModel = "spring") MapStruct interfaces
├── repository   # JpaRepository<T, Long> per aggregate
└── service      # @Service classes with @Transactional methods
```
