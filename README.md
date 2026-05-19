# AcademConnect

Plataforma web para gestión académica de TCCs y tesis con asignación inteligente de revisores.

## Tecnologías

- Java 25, Spring Boot 4.0.6
- PostgreSQL 16, Flyway, Hibernate/JPA
- Spring Security + JWT (HMAC-SHA256)
- MapStruct, Lombok
- Testcontainers (tests de integración)

## Desarrollo local

### Prerequisitos

- Java 25+
- Docker + Docker Compose

### Levantar base de datos

```bash
docker compose up -d
```

### Ejecutar la aplicación

```bash
./mvnw spring-boot:run
```

La app arranca en `http://localhost:8080`. Las migraciones Flyway se ejecutan automáticamente.

### Ejecutar tests

```bash
./mvnw test
```

Los tests usan Testcontainers (requiere Docker). No se necesita base de datos externa.

## Variables de entorno

| Variable | Default | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/academconnect` | URL de conexión |
| `DB_USER` | `academconnect` | Usuario de DB |
| `DB_PASSWORD` | `academconnect` | Contraseña de DB |
| `JWT_SECRET` | `dev-secret-academconnect-needs-32-chars!` | Clave HMAC-SHA256 (mínimo 32 chars) |
| `JWT_EXPIRATION` | `86400` | Expiración del token en segundos |
| `STORAGE_ROOT` | `./data/documents` | Directorio de almacenamiento de PDFs |

## Despliegue con Docker

### Construir imagen

```bash
docker build -t academconnect:latest .
```

### Ejecutar contenedor

```bash
docker run -d \
  -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://db:5432/academconnect \
  -e DB_USER=academconnect \
  -e DB_PASSWORD=<contraseña-segura> \
  -e JWT_SECRET=<secreto-minimo-32-chars> \
  -v academconnect-docs:/app/data/documents \
  --name academconnect \
  academconnect:latest
```

### Docker Compose completo (app + DB)

```yaml
services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: academconnect
      POSTGRES_USER: academconnect
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - pg-data:/var/lib/postgresql/data

  app:
    image: academconnect:latest
    ports:
      - "8080:8080"
    environment:
      DB_URL: jdbc:postgresql://db:5432/academconnect
      DB_USER: academconnect
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
    volumes:
      - doc-storage:/app/data/documents
    depends_on:
      - db

volumes:
  pg-data:
  doc-storage:
```

## Endpoints principales

### Autenticación

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/auth/login` | Login — devuelve JWT |
| POST | `/auth/register/estudiante` | Registro de estudiante |
| POST | `/auth/register/profesor` | Registro de profesor |
| POST | `/auth/register/externo` | Registro de evaluador externo |

### Trabajos y versionamiento

| Método | Ruta | Rol |
|---|---|---|
| GET | `/api/trabajos` | Autenticado |
| POST | `/api/trabajos` | PROFESOR |
| POST | `/api/trabajos/{id}/versiones` | ESTUDIANTE, PROFESOR |
| GET | `/api/trabajos/{id}/versiones/{vId}/documento` | Autenticado |

### Evaluaciones

| Método | Ruta | Rol |
|---|---|---|
| POST | `/api/trabajos/{id}/sugerir-revisores?k=3` | ADMINISTRADOR |
| POST | `/api/asignaciones` | ADMINISTRADOR |
| POST | `/api/evaluaciones` | PROFESOR, EXTERNO |
| GET | `/evaluador/me/asignaciones` | PROFESOR, EXTERNO |
| GET | `/estudiante/me/trabajos/{id}/nota` | ESTUDIANTE |

### Métricas

| Método | Ruta | Rol |
|---|---|---|
| GET | `/admin/metricas` | ADMINISTRADOR |

## Algoritmo de recomendación de revisores

El algoritmo implementa `score = w₁·afinidad + w₂·(1−carga_norm) + w₃·disponibilidad` donde:

- **afinidad**: similitud de Jaccard entre las áreas temáticas del trabajo y las del candidato.
- **carga_norm**: asignaciones activas del candidato / máximo entre todos los candidatos.
- **disponibilidad**: 1.0 (fijo en esta versión).

Pesos configurables vía:
```properties
academconnect.algoritmo.w1=0.6
academconnect.algoritmo.w2=0.3
academconnect.algoritmo.w3=0.1
```
