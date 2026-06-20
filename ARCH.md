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
- **Horizontal (layer):** within a feature, the usual layers (Controller, Service,
  Repository, Entity, DTO).

Truly cross-cutting infrastructure (security wiring, typed config) lives in `config`.

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
│   ├── AuthController.java             # REST: GET /auth/me, POST /auth/logout
│   ├── AuthService.java               # find-or-create user, getUserById (@Transactional)
│   ├── CustomOAuth2UserService.java   # loads Google user, provisions it in DB
│   ├── OAuth2LoginSuccessHandler.java # issues JWT cookie + redirect after OAuth login
│   ├── JwtService.java                # create/parse/verify JWT (HS256)
│   ├── JwtAuthenticationFilter.java   # reads access_token cookie -> SecurityContext
│   ├── JwtUserPrincipal.java          # authenticated principal (record: userId, email)
│   ├── CookieUtils.java               # build/clear the access_token cookie
│   └── GoogleUserProfile.java         # maps OAuth2 attributes (sub/email/name...)
│
├── user/                               # User domain
│   ├── UserEntity.java                # JPA entity -> table "users"
│   ├── UserRepository.java            # Spring Data JPA repo
│   ├── UserResponse.java              # response DTO (record) + from(entity) mapper
│   └── UserNotFoundException.java     # thrown by AuthService.getUserById
│
├── channel/                            # Channel domain
│   ├── ChannelEntity.java             # -> table "channels"  (createdBy -> UserEntity)
│   ├── ChannelMemberEntity.java       # -> table "channel_members" (unique user+channel)
│   ├── ChannelRepository.java
│   ├── ChannelMemberRepository.java
│   └── ChannelType.java               # enum: PUBLIC, PRIVATE, DM
│
├── message/                            # Message domain
│   ├── MessageEntity.java             # -> table "messages" (user + nullable channel)
│   └── MessageRepository.java
│
└── config/                             # Cross-cutting infrastructure
    ├── SecurityConfig.java            # Spring Security filter chain, CORS, CSRF
    ├── AppProperties.java             # typed config (@ConfigurationProperties "app")
    └── AppConfig.java                 # @EnableConfigurationProperties(AppProperties)
```

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
| (auto) | `/oauth2/**`, `/login/**` | No | Spring Security OAuth2 login endpoints. |
| (auto) | `/swagger-ui.html`, `/v3/api-docs/**` | No | OpenAPI / Swagger UI. |

\* logout requires a valid CSRF token (cookie-based) like other state-changing requests.

> The `channel` and `message` features currently have **persistence only**
> (entities + repositories). Their services and controllers are not implemented
> yet — this is the main area of upcoming work.

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

Tests mirror the main package structure (feature-first) and total **71 tests**,
in three styles:

- **Unit (Mockito):** pure logic, no Spring context — e.g. `AuthServiceTest`,
  `JwtServiceTest`, `CookieUtilsTest`, `JwtAuthenticationFilterTest`,
  `CustomOAuth2UserServiceTest`, `OAuth2LoginSuccessHandlerTest`,
  `GoogleUserProfileTest`, `UserResponseTest`.
- **Persistence slice (`@DataJpaTest` + H2):** repository query methods and
  constraints — `UserRepositoryTest`, `ChannelRepositoryTest`,
  `ChannelMemberRepositoryTest`, `MessageRepositoryTest`.
- **Integration (`@SpringBootTest` + MockMvc):** end-to-end auth flow —
  `AuthControllerTest`.

Shared test data builders live in `support/RepositoryTestFixtures` (a `public`
helper usable across feature test packages).

Test layout:
```
src/test/java/com/motionnblur/senkron_backend/
├── auth/        # unit + integration tests for the auth feature
├── user/        # UserRepositoryTest, UserResponseTest
├── channel/     # ChannelRepositoryTest, ChannelMemberRepositoryTest
├── message/     # MessageRepositoryTest
├── support/     # RepositoryTestFixtures (shared)
└── SenkronBackendApplicationTests.java  # context loads
```

Run tests: `./mvnw test` · Compile only: `./mvnw clean test-compile`

---

## 9. Conventions (follow these when extending)

- **New feature** → new top-level package (e.g. `channel`) holding its controller,
  service, repository, entity, and DTOs.
- **Layering inside a feature:** Controller (web) → Service (business logic,
  `@Transactional` where state changes) → Repository (Spring Data JPA) → Entity.
- **Never expose entities over HTTP.** Add a response DTO (record) with a static
  `from(entity)` mapper, like `UserResponse`.
- **Read the authenticated user** with `@AuthenticationPrincipal JwtUserPrincipal`.
- **Config:** add typed fields to `AppProperties` instead of using raw `@Value`.
- **Cross-feature use** is via explicit imports (e.g. a `channel` service importing
  `user.UserRepository`). Keep genuinely shared infra in `config`.
- **Constructor injection** everywhere (no field injection).

---

## 10. Roadmap / known gaps

These are intentional and not yet implemented:

- **Channel/Message business layer:** no services or controllers yet (only persistence).
- **Global exception handling:** no `@RestControllerAdvice`; `UserNotFoundException`
  is thrown but there is no central HTTP error mapping (e.g. → 404).
- **Refresh tokens:** only a single access-token JWT cookie exists today.
- **Real-time messaging:** no WebSocket/STOMP transport yet.
- **Module boundary enforcement:** feature packages are not yet sealed
  (everything is `public`).
```
