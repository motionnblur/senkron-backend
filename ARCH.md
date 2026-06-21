# Senkron Backend — Architecture (ARCH.md)

> Purpose: a single document that lets a developer (or an AI assistant) understand
> this project instantly — how it is organized, how requests flow, where things
> live, and the conventions to follow when extending it.

---

## 1. What this project is

Senkron Backend is a **Spring Boot REST API** for a chat-style application
(users, channels, messages). Authentication is handled via **Google OAuth2
login** that issues a **JWT stored in an HttpOnly cookie**; subsequent requests
are authenticated statelessly by reading and verifying that cookie.

- **Language / Runtime:** Java 21
- **Framework:** Spring Boot 4.1.0 (Spring MVC, Spring Security, Spring Data JPA)
- **Build tool:** Maven (wrapper: `./mvnw`)
- **Database:** PostgreSQL (runtime), H2 (tests only)
- **API docs:** springdoc-openapi (Swagger UI)
- **Auth:** Spring Security OAuth2 Client + custom JWT (jjwt 0.12.6)
- **Boilerplate reduction:** Lombok

---

## 2. Architectural style: Package-by-Feature (feature-first)

The codebase is organized **by business feature**, not by technical layer.
Each top-level package is a domain/feature and contains everything that feature
needs (web controller, service/business logic, persistence, DTOs, exceptions).
Classic layering (Controller → Service → Repository → Entity) still exists, but
it lives *inside* each feature package rather than in global `controller/`,
`service/`, `repository/` packages.

Think of it as two axes:

- **Vertical (feature):** `auth`, `user`, `channel`, `message` — top-level packages.
- **Horizontal (layer):** within a feature, consistent sub-packages:
  `api`, `service`, `domain`, `repository`, `dto/request`, `dto/response`, `exception`.
  Only create sub-packages that have files (e.g. `message` currently has `domain` + `repository` only).

Truly cross-cutting infrastructure lives in `config` with adapted sub-packages:
`security`, `properties`, `web`.

### Why this style
- High cohesion: everything about a feature is in one place.
- Low coupling between features; cross-feature dependencies are explicit imports.
- Adding a new domain is mechanical (create a new feature package).
- Easy to later extract a feature into its own module/service.

### Known, accepted trade-offs
- Cross-feature dependencies exist and are allowed: `auth → user`,
  `channel → user`, `message → user` and `message → channel`.
- All classes are currently `public` (no enforced module boundaries yet). If the
  project grows, feature boundaries can be hardened with package-private
  visibility or Spring Modulith.

---

## 3. Package map

```
com.motionnblur.senkron_backend
├── SenkronBackendApplication.java      # Spring Boot entry point (@SpringBootApplication)
│
├── auth/                               # Authentication & security feature
│   ├── api/
│   │   └── AuthController.java         # REST: GET /auth/me, POST /auth/logout
│   ├── service/
│   │   ├── AuthService.java            # find-or-create user, getUserById (@Transactional)
│   │   ├── CustomOAuth2UserService.java
│   │   ├── OAuth2LoginSuccessHandler.java
│   │   ├── JwtService.java
│   │   ├── JwtAuthenticationFilter.java
│   │   └── CookieUtils.java
│   └── domain/
│       ├── JwtUserPrincipal.java       # authenticated principal (cross-feature public API)
│       └── GoogleUserProfile.java
│
├── user/                               # User domain
│   ├── domain/
│   │   └── UserEntity.java             # JPA entity -> table "users"
│   ├── repository/
│   │   └── UserRepository.java
│   ├── dto/response/
│   │   └── UserResponse.java           # response DTO (record) + from(entity) mapper
│   └── exception/
│       └── UserNotFoundException.java
│
├── channel/                            # Channel domain
│   ├── api/
│   │   └── ChannelController.java      # REST: POST /channels, join/leave/members
│   ├── service/
│   │   └── ChannelService.java
│   ├── domain/
│   │   ├── ChannelEntity.java
│   │   ├── ChannelMemberEntity.java
│   │   └── ChannelType.java            # enum: PUBLIC, PRIVATE, DM
│   ├── repository/
│   │   ├── ChannelRepository.java
│   │   └── ChannelMemberRepository.java
│   ├── dto/request/
│   │   ├── CreateChannelRequest.java
│   │   └── AddChannelMemberRequest.java
│   ├── dto/response/
│   │   └── ChannelResponse.java
│   └── exception/
│       ├── ChannelNotFoundException.java
│       └── ChannelMemberNotFoundException.java
│
├── message/                            # Message domain (persistence only today)
│   ├── domain/
│   │   └── MessageEntity.java
│   └── repository/
│       └── MessageRepository.java
│
└── config/                             # Cross-cutting infrastructure
    ├── security/
    │   └── SecurityConfig.java         # Spring Security filter chain, CORS, CSRF
    ├── properties/
    │   ├── AppProperties.java          # typed config (@ConfigurationProperties "app")
    │   └── AppConfig.java
    └── web/
        └── GlobalExceptionHandler.java # @RestControllerAdvice -> ProblemDetail responses
```

### Cross-feature public API (import these from other features)

- `auth.domain.JwtUserPrincipal` — read authenticated user in controllers
- `user.dto.response.UserResponse` — user HTTP responses
- `user.domain.UserEntity`, `user.repository.UserRepository`, `user.exception.UserNotFoundException`
- `channel.domain.*`, `channel.exception.*` — entity relations and error mapping
- `auth.service.*` — wired by `SecurityConfig`
- `config.properties.AppProperties` — typed app configuration

---

## 4. Data model

All entities use `@Data` (Lombok), `IDENTITY` generated IDs, and `LocalDateTime`
timestamps. Relationships are `@ManyToOne(fetch = LAZY)`.

| Entity                | Table             | Key fields / relations                                                                 |
|-----------------------|-------------------|-----------------------------------------------------------------------------------------|
| `UserEntity`          | `users`           | `email` (unique, not null), `googleId` (unique, nullable), `password` (nullable), `name`, `lastName`, `displayName`, `createdAt` |
| `ChannelEntity`       | `channels`        | `name`, `description`, `type` (enum string, nullable), `createdBy → UserEntity` (not null), `createdAt` |
| `ChannelMemberEntity` | `channel_members` | `user → UserEntity`, `channel → ChannelEntity`, `joinedAt`; **unique (user_id, channel_id)** |
| `MessageEntity`       | `messages`        | `content`, `createdAt`, `user → UserEntity` (not null), `channel → ChannelEntity` (nullable = supports DMs) |

Notes:
- `password` is nullable because OAuth-only users have no password.
- `MessageEntity.channel` is nullable to allow channel-less (direct) messages.

### Repositories (query methods present today)
- `UserRepository`: `findByEmail`, `findByGoogleId`, `existsByEmail`
- `ChannelRepository`: `findByType`, `findByCreatedBy`, `findByCreatedById`
- `ChannelMemberRepository`: `findByUser`, `findByUserId`, `findByChannel`,
  `findByChannelId`, `findByUserAndChannel`, `existsByUserIdAndChannelId`
- `MessageRepository`: `findByChannelOrderByCreatedAtDesc`,
  `findByChannelIdOrderByCreatedAtDesc(Pageable)`, `findByUser`

---

## 5. Authentication & security (the core flow)

### 5.1 Login (Google OAuth2 → JWT cookie)
1. User initiates Google OAuth2 login (Spring Security `oauth2Login`).
2. `CustomOAuth2UserService.loadUser` runs: fetches the Google profile and calls
   `AuthService.findOrCreateGoogleUser(...)` to provision/link the user in the DB.
3. On success, `OAuth2LoginSuccessHandler`:
   - calls `AuthService.findOrCreateGoogleUser(...)` to get the persisted user,
   - generates a JWT via `JwtService.generateToken(userId, email)`,
   - writes it as an **HttpOnly `access_token` cookie** (`CookieUtils`),
   - invalidates the temporary HTTP session,
   - redirects to the configured frontend URL (`app.oauth2.success-url`).

### 5.2 Authenticated requests (stateless)
1. `JwtAuthenticationFilter` (runs before `UsernamePasswordAuthenticationFilter`)
   reads the `access_token` cookie.
2. `JwtService.parseToken` verifies signature/expiry and returns a
   `JwtUserPrincipal(userId, email)` (or `null` if invalid).
3. The principal is placed in the `SecurityContext`; controllers read it via
   `@AuthenticationPrincipal JwtUserPrincipal`.

### 5.3 Logout
- `POST /auth/logout` clears the `access_token` cookie (max-age 0).

### 5.4 JWT details (`JwtService`)
- Algorithm: HMAC-SHA (`Keys.hmacShaKeyFor(secret.getBytes())`).
- Claims: `subject = userId`, custom claim `email`, `issuedAt`, `expiration`.
- Expiry: `app.jwt.expiration-hours` (converted to ms).
- Invalid/expired/tampered/non-numeric-subject tokens → `parseToken` returns `null`.

### 5.5 Spring Security config (`SecurityConfig`)
- **CORS:** origins from `app.cors.allowed-origins` (comma-separated), credentials allowed.
- **CSRF:** `CookieCsrfTokenRepository.withHttpOnlyFalse()` (cookie-based CSRF token).
- **Sessions:** `IF_REQUIRED` (needed transiently during the OAuth2 handshake).
- **Public endpoints:** `/oauth2/**`, `/login/**`, `/error`, `/v3/api-docs/**`,
  `/swagger-ui/**`, `/swagger-ui.html`. Everything else requires authentication.
- **Unauthorized handling:** returns `401` (`HttpStatusEntryPoint(UNAUTHORIZED)`).
- **Filter:** `JwtAuthenticationFilter` added before `UsernamePasswordAuthenticationFilter`.

---

## 6. HTTP API (current surface)

| Method | Path           | Auth | Description                                  |
|--------|----------------|------|----------------------------------------------|
| GET    | `/auth/me`     | Yes  | Returns the current user as `UserResponse`.  |
| POST   | `/auth/logout` | Yes* | Clears the `access_token` cookie.            |
| POST   | `/channels`    | Yes* | Creates a PUBLIC or PRIVATE channel; creator is added as the first member. Returns `ChannelResponse` (`201`). |
| (auto) | `/oauth2/**`, `/login/**` | No | Spring Security OAuth2 login endpoints. |
| (auto) | `/swagger-ui.html`, `/v3/api-docs/**` | No | OpenAPI / Swagger UI. |

\* state-changing requests require a valid CSRF token (cookie-based), including logout and `POST /channels`.

### 6.1 Channel creation flow (`POST /channels`)

1. `ChannelController` reads `@AuthenticationPrincipal JwtUserPrincipal` for the creator's `userId`.
2. `ChannelService.createChannel(userId, request)` runs in a single `@Transactional` boundary:
   - loads `UserEntity` (throws `UserNotFoundException` if missing),
   - validates request (`name`/`description` not blank, `type` not null, `type != DM`),
   - saves `ChannelEntity`,
   - saves `ChannelMemberEntity` for the creator.
3. Controller maps the saved entity to `ChannelResponse` and returns `201 Created`.

Request body (`CreateChannelRequest`): `name`, `description`, `type` (`PUBLIC` or `PRIVATE`).

> The `message` feature still has **persistence only** (entity + repository).
> Channel listing, membership management, and message APIs are the main upcoming work.

---

## 7. Configuration

All custom config is bound to the `app.*` prefix via the `AppProperties` record
(`@ConfigurationProperties(prefix = "app")`), enabled by `AppConfig`.

```
app.jwt.secret              # HMAC secret (>= 32 bytes)
app.jwt.expiration-hours    # token lifetime in hours
app.cookie.secure           # mark cookie Secure (true in prod/HTTPS)
app.cookie.same-site        # SameSite policy (e.g. Lax / Strict / None)
app.oauth2.success-url      # frontend URL to redirect to after login
app.cors.allowed-origins    # comma-separated allowed origins
```

Standard Spring properties (datasource, Google OAuth2 client id/secret, JPA)
live in `application.properties` / `application-local.properties`. Secrets are
kept out of version control (see `.gitignore`).

---

## 8. Testing strategy

Tests mirror the main package structure (feature-first) and total **76 tests**,
in three styles:

- **Unit (Mockito):** pure logic, no Spring context — e.g. `AuthServiceTest`,
  `ChannelServiceTest`, `JwtServiceTest`, `CookieUtilsTest`,
  `JwtAuthenticationFilterTest`, `CustomOAuth2UserServiceTest`,
  `OAuth2LoginSuccessHandlerTest`, `GoogleUserProfileTest`, `UserResponseTest`.
- **Persistence slice (`@DataJpaTest` + H2):** repository query methods and
  constraints — `UserRepositoryTest`, `ChannelRepositoryTest`,
  `ChannelMemberRepositoryTest`, `MessageRepositoryTest`.
- **Integration (`@SpringBootTest` + MockMvc):** end-to-end HTTP flows —
  `AuthControllerTest`, `ChannelControllerTest`.

Shared test data builders live in `support/RepositoryTestFixtures` (a `public`
helper usable across feature test packages).

Test layout (mirrors main layer sub-packages):
```
src/test/java/com/motionnblur/senkron_backend/
├── auth/
│   ├── api/           AuthControllerTest
│   ├── service/       AuthServiceTest, JwtServiceTest, CookieUtilsTest, ...
│   └── domain/        GoogleUserProfileTest
├── user/
│   ├── repository/    UserRepositoryTest
│   └── dto/response/  UserResponseTest
├── channel/
│   ├── api/           ChannelControllerTest
│   ├── service/       ChannelServiceTest
│   └── repository/    ChannelRepositoryTest, ChannelMemberRepositoryTest
├── message/
│   └── repository/    MessageRepositoryTest
├── support/           RepositoryTestFixtures (shared)
└── SenkronBackendApplicationTests.java
```

Run tests: `./mvnw test` · Compile only: `./mvnw clean test-compile`

---

## 9. Conventions (follow these when extending)

- **New feature** → new top-level package with layer sub-packages:
  `api`, `service`, `domain`, `repository`, `dto/request`, `dto/response`, `exception`
  (create only the layers you need).
- **Layering inside a feature:** `api` → `service` → `repository` → `domain`;
  HTTP DTOs live under `dto/` and must not expose entities directly.
- **Never expose entities over HTTP.** Add a response DTO (record) with a static
  `from(entity)` mapper under `dto/response`, like `UserResponse`.
- **Read the authenticated user** with `@AuthenticationPrincipal JwtUserPrincipal`
  (from `auth.domain`).
- **Config:** add typed fields to `config.properties.AppProperties` instead of raw `@Value`.
- **Cross-feature use** is via explicit imports (e.g. `channel.service` importing
  `user.repository.UserRepository`). Keep genuinely shared infra in `config`.
- **Constructor injection** everywhere (no field injection).

---

## 10. Roadmap / known gaps

These are intentional and not yet implemented:

- **Channel APIs beyond creation:** list channels, DM creation.
- **Message business layer:** no service or controller yet (persistence only).
- **Global exception handling:** `config.web.GlobalExceptionHandler` maps domain
  not-found exceptions to `404`, validation errors to `400`, and state violations to `403`.
- **Workspace model:** channels are not scoped to a team/workspace yet.
- **Refresh tokens:** only a single access-token JWT cookie exists today.
- **Real-time messaging:** no WebSocket/STOMP transport yet.
- **Module boundary enforcement:** feature packages are not yet sealed
  (everything is `public`).
