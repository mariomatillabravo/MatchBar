# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MatchBar is a full-stack app for finding bars that broadcast football matches. It consists of:
- **`/backend`** — Spring Boot 3.3 REST API (Java 21, Maven, MongoDB)
- **`/android`** — Kotlin + Jetpack Compose Android client (Gradle KTS, minSdk 26)

## Build & Run Commands

### Backend

```bash
# Run with dev profile (MongoDB via Docker Compose)
cd backend
./mvnw spring-boot:run

# Run with Docker (includes MongoDB 7.0)
docker compose up --build

# Run tests
cd backend && ./mvnw test
```

Backend runs on `http://localhost:8080`. Swagger UI at `/swagger-ui.html`, admin panel at `/admin.html`.

### Android

Open `/android` in Android Studio and run on emulator or device.

- Emulator uses `10.0.2.2:8080` to reach localhost backend.
- For a physical device, update `API_BASE_URL` in `app/build.gradle.kts` to your machine's LAN IP.

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
- **Seeding**: `DataSeeder.java` pre-populates MongoDB with test users, bars, matches, competitions, and teams on startup (dev profile).

Key files: `SecurityConfig.java`, `AuthService.java` (JWT generation/validation), `BarService.java` (geospatial logic), `application.yml` (MongoDB URI, JWT secret, port).

### MongoDB Collections

`users`, `bars`, `matches`, `competitions`, `teams`, `broadcasts`, `reviews`, `favorites`, `incidents`.

Bars reference a user (`userId`) for ownership. Matches reference competitions and teams via `@DBRef`.

## Test Users (dev/seeded data)

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
