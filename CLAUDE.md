# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MatchBar is a full-stack app for finding bars that broadcast football matches. It consists of:
- **`/backend`** — Spring Boot 4.1 REST API (Java 21, Maven, MongoDB, Jackson 3)
- **`/android`** — Kotlin + Jetpack Compose Android client (Gradle KTS, minSdk 26)

## Build & Run Commands

### Backend

```bash
# First time: create .env from the template and fill in passwords + JWT_SECRET
cp .env.example .env

# Run with Docker (MongoDB 7.0 with auth + API). Profile comes from SPRING_PROFILES_ACTIVE in .env
docker compose up --build

# Run with Maven against the Mongo container (dev profile reads the root .env)
docker compose up -d mongo
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Run tests (no MongoDB needed)
cd backend && ./mvnw verify
```

Use the wrappers (`backend/mvnw`, `android/gradlew`; `.cmd`/`.bat` on Windows) — they pin Maven 3.9.9 and Gradle 8.7 with checksums. Only JDK 21 is required locally.

### CI and secret scanning

`.github/workflows/ci.yml` runs on every PR and push to `main`: backend `./mvnw verify`, Android `assembleDebug testDebugUnitTest`, and gitleaks over the full git history (`.gitleaks.toml` adds a football-data token rule). `.gitleaksignore` lists only already-rotated historical findings — never add a live secret there; rotate it instead.

Backend runs on `http://localhost:8080`. Admin panel at `/admin.html`. Swagger UI at `/swagger-ui.html` only in the `dev` profile (or `SWAGGER_ENABLED=true`). CORS is closed by default (`CORS_ALLOWED_ORIGINS`); the admin panel is same-origin.

Secrets (`JWT_SECRET`, `FOOTBALL_API_KEY`, Mongo credentials, `MATCHBAR_ADMIN_*`) come only from env vars — never add defaults for them in `application.yml`. The API refuses to start without a ≥32-byte `JWT_SECRET`.

### Android

Open `/android` in Android Studio and run on emulator or device.

- Debug builds use `http://10.0.2.2:8080/` (emulator → host localhost); cleartext is allowed only via `src/debug/res/xml/network_security_config.xml`.
- For a physical device, change the `debug` `API_BASE_URL` in `app/build.gradle.kts` to your LAN IP, add it to the debug network security config, and set `API_BIND=0.0.0.0` in `.env`.
- Release builds require `-Pmatchbar.releaseApiUrl=https://...`; `preReleaseBuild` fails otherwise. Release = R8 minify + resource shrinking; signing comes from `matchbar.keystore.*`/`matchbar.key.*` Gradle properties or `MATCHBAR_KEYSTORE_*` env vars (unsigned if absent). Never commit `*.jks`/`*.keystore`.
- Toolchain: AGP 9.4 with built-in Kotlin (do NOT apply `org.jetbrains.kotlin.android`; no `kotlinOptions` — jvmTarget follows `compileOptions`), Gradle 9.8, Kotlin 2.4, compileSdk/targetSdk 36 (Play minimum since 2026-08-31).
- Edge-to-edge is mandatory at targetSdk 35+: `MainActivity` calls `enableEdgeToEdge()`; the outer Scaffold in `AppNavigation` uses `contentWindowInsets = WindowInsets(0)` and the NavHost consumes its padding + `imePadding()`. Screens without a Scaffold must add `systemBarsPadding()`.

## Architecture

### Android (MVVM)

- **UI**: Screens are `@Composable` functions in `ui/screens/`; state lives in `ViewModel` subclasses.
- **Navigation**: Single `NavHost` in `AppNavigation.kt` with string routes defined in `Routes.kt`. Bottom nav appears only for `USER` role.
- **Networking**: `NetworkModule.kt` builds a Retrofit instance with an OkHttp interceptor that injects `Authorization: Bearer <token>` from `SessionStore`. All API methods are declared in `MatchBarApi.kt`.
- **Session**: `SessionStore.kt` (DataStore) persists the JWT token and the logged-in user's role/id across app restarts.
- **Models**: Kotlinx Serialization data classes in `Models.kt` — not GSON.

Key files: `MainActivity.kt` (entry point, location permissions), `MatchBarApp.kt` (Application class), `AppNavigation.kt`, `SessionStore.kt`, `MatchBarApi.kt`, `NetworkModule.kt`.

### Backend (Layered Spring Boot)

```
Controller → Service → Repository → MongoDB
```

- **Security**: `JwtAuthFilter` validates tokens before each request; `SecurityConfig` defines public vs. protected routes and CORS.
- **Roles**: `USER`, `BAR`, `ADMIN`. Endpoints are protected with `@PreAuthorize` or security matchers.
- **Geospatial**: Bars have a `GeoJsonPoint` location field with a 2D sphere index. `BarService` uses `NearQuery` for proximity searches.
- **Seeding**: `DataSeeder.java` (`@Profile("dev")` only) pre-populates MongoDB with test users, bars, matches, competitions, and teams on startup. Outside `dev`, `AdminBootstrap.java` creates the first ADMIN from `MATCHBAR_ADMIN_EMAIL`/`MATCHBAR_ADMIN_PASSWORD`.
- **Errors**: `GlobalExceptionHandler` maps exceptions to 4xx with the shared `ErrorResponses` JSON body; `JsonSecurityErrorHandler` returns 401 (missing/expired token) vs 403 (wrong role). Throw `ApiException` for business errors.
- **Spring Boot 4 conventions**: JSON is Jackson 3 (`tools.jackson.databind.*`; annotations stay in `com.fasterxml.jackson.annotation`). Tests use `@MockitoBean` (not `@MockBean`) and `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`. Mongo connection is `spring.mongodb.uri` (not `spring.data.mongodb.uri`). `ConfigurationPropertiesTest` fails if any key in `application*.yml` was retired by Spring — run it after every Spring Boot upgrade.

Key files: `SecurityConfig.java`, `AuthService.java` (JWT generation/validation), `BarService.java` (geospatial logic), `application.yml` (MongoDB URI, JWT secret, port).

### MongoDB Collections

`users`, `bars`, `matches`, `competitions`, `teams`, `broadcasts`, `reviews`, `favorites`, `incidents`.

Bars reference a user (`userId`) for ownership. Matches reference competitions and teams via `@DBRef`.

## Test Users (seeded only with the `dev` profile)

| Email | Role | Password |
|---|---|---|
| `admin@matchbar.com` | ADMIN | `password123` |
| `mario@test.com` | USER | `password123` |
| `rincon@test.com` | BAR | `password123` |
| `penalti@test.com` | BAR (PENDING) | `password123` |

## API Contract

Android calls the backend using these main endpoint groups:
- `POST /api/auth/login`, `/api/auth/register`
- `GET /api/matches` — match list with filters
- `GET /api/bars/nearby?lat=&lng=` — geospatial query
- `GET /api/bars/{id}`, `POST /api/bars/{id}/reviews`
- `GET/POST /api/users/me/favorites`
- `GET /api/admin/bars/pending`, `PATCH /api/admin/bars/{id}/approve`
- `GET /api/bar/me` — bar owner's own bar data

## Navigation Routes (Android)

`login`, `register`, `matches`, `map?matchId={id}`, `bar/{barId}`, `favorites`, `profile`, `my-bar`, `admin/pending`, `incidents`
