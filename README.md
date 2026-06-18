# senkron-backend

Spring Boot backend for Senkron.

## Prerequisites

- **Java 21**
- **Docker Desktop** (for local PostgreSQL)

## First-time setup

From the project root:

**1. Start PostgreSQL**

```bash
docker compose up -d
```

**2. Create local config**

```bash
# Git Bash / macOS / Linux
cp src/main/resources/application-local.properties.example \
   src/main/resources/application-local.properties

# Windows (PowerShell)
Copy-Item src/main/resources/application-local.properties.example `
          src/main/resources/application-local.properties
```

**3. Add OAuth credentials**

Edit `src/main/resources/application-local.properties` and replace the placeholders with real values from your team lead or password manager:

```properties
spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET
```

### Google OAuth redirect URI

In [Google Cloud Console](https://console.cloud.google.com/) → **APIs & Services** → **Credentials**, add this authorized redirect URI to your OAuth client:

```
http://localhost:8080/login/oauth2/code/google
```

## Run the app

### IDE (VS Code / Cursor)

1. Open **Run and Debug** (`Ctrl+Shift+D`)
2. Select **SenkronBackend (local)**
3. Press Run

This starts the app with the `local` profile on port `8080`.

### Command line

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## Run tests

Tests use an in-memory H2 database — PostgreSQL is not required:

```bash
./mvnw test
```

## Local services

| Service    | URL / Port                          |
| ---------- | ----------------------------------- |
| Backend    | http://localhost:8080               |
| PostgreSQL | localhost:5432 (db: `senkron`)    |

Start/stop Postgres:

```bash
docker compose up -d      # start
docker compose stop       # stop
docker compose logs -f    # view logs
```

## Configuration files

| File | Purpose |
| ---- | ------- |
| `application.properties` | Shared defaults (safe to commit) |
| `application-local.properties.example` | Template for local secrets |
| `application-local.properties` | Your local secrets (gitignored) |
| `compose.yml` | Local PostgreSQL container |
