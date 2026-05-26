# MatchBar — Contexto de proyecto para LLMs

Aplicación móvil para localizar bares que retransmiten partidos de fútbol en tiempo real.
Proyecto de Fin de Ciclo DAM — IES Tetuán de las Victorias (2025/2026).
Equipo: Roberto Fernández, Roberto Asperilla, Mario Matilla Bravo.

---

## Stack tecnológico

| Capa | Tecnología |
|------|-----------|
| Backend | Spring Boot 3.3, Java 21, Maven |
| Base de datos | MongoDB 7.0 (prod) |
| Seguridad | Spring Security + JWT (JJWT 0.12.6) |
| API Docs | SpringDoc OpenAPI 3 / Swagger UI |
| Android | Kotlin 2.0.21, Jetpack Compose, Gradle KTS |
| Networking (Android) | Retrofit 2.11 + OkHttp 4.12 + Kotlinx Serialization |
| Mapas | Google Maps Compose 4.4 |
| Persistencia local | DataStore Preferences |
| Imágenes | Coil 2.7 |
| Despliegue | Docker + docker-compose |

---

## Estructura del repositorio

```
matchbar_v1/
├── CLAUDE.md
├── README.md
├── docker-compose.yml
├── backend/                          # API REST Spring Boot
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/matchbar/
│       │   ├── config/               # SecurityConfig, MongoConfig, DataSeeder
│       │   ├── controller/           # AuthController, BarController, MatchController,
│       │   │                         # FavoriteController, AdminController,
│       │   │                         # IncidentController, PublicController
│       │   ├── dto/
│       │   │   ├── request/          # LoginRequest, RegisterRequest, BarUpsertRequest,
│       │   │   │                     # ReviewRequest, IncidentRequest
│       │   │   └── response/         # AuthResponse, BarResponse, MatchResponse,
│       │   │                         # ReviewResponse, IncidentResponse
│       │   ├── entity/               # User, Bar, Match, Team, Competition,
│       │   │                         # Review, Broadcast, Favorite, Incident
│       │   ├── exception/            # ApiException, GlobalExceptionHandler
│       │   ├── repository/           # Spring Data MongoDB (un repo por entidad)
│       │   ├── security/             # JwtTokenProvider, JwtAuthFilter,
│       │   │                         # UserPrincipal, UserDetailsServiceImpl
│       │   └── service/              # AuthService, BarService, MatchService,
│       │                             # ReviewService, FavoriteService, IncidentService
│       └── resources/
│           ├── application.yml
│           └── static/admin.html     # Panel admin (SPA estática)
└── android/
    └── app/src/main/java/com/matchbar/app/
        ├── data/
        │   ├── api/                  # MatchBarApi.kt (Retrofit), NetworkModule.kt
        │   ├── local/                # SessionStore.kt (JWT + user en DataStore)
        │   └── model/                # Models.kt (data classes Kotlinx Serialization)
        ├── ui/
        │   ├── navigation/           # AppNavigation.kt, Routes.kt
        │   ├── screens/
        │   │   ├── auth/             # LoginScreen, RegisterScreen, AuthViewModel
        │   │   ├── matches/          # MatchesScreen, MatchesViewModel
        │   │   ├── map/              # NearbyBarsScreen, NearbyBarsViewModel
        │   │   ├── bars/             # BarDetailScreen, BarDetailViewModel
        │   │   ├── bar/              # MyBarScreen (rol BAR)
        │   │   ├── favorites/        # FavoritesScreen
        │   │   ├── profile/          # ProfileScreen
        │   │   └── admin/            # AdminPendingScreen (rol ADMIN)
        │   ├── common/               # Components.kt, GenericFactory.kt
        │   └── theme/                # Theme.kt (Material 3)
        ├── util/                     # Utils.kt
        ├── MainActivity.kt
        └── MatchBarApp.kt
```

---

## Modelo de dominio

```
User ──favoritos──► Bar ◄──broadcasts──► Match
 │                   │                      │
 └──reviews──────────┘            Team ─────┘
                                  Competition
                                  Incident
```

### Entidades principales

**User** — email, password (BCrypt), role (`USER` | `BAR` | `ADMIN`)

**Bar** — name, address, lat/lng, phone, website, description, logoUrl, rating, status (`PENDING` | `APPROVED`), ownerId

**Match** — homeTeam, awayTeam, competition, dateTime, status (`UPCOMING` | `ONGOING` | `FINISHED`), venue

**Review** — barId, userId, rating (1-5), comment, createdAt

**Broadcast** — barId, matchId (qué bar emite qué partido)

**Favorite** — userId, barId

**Incident** — matchId, type (GOAL | CARD | SUBSTITUTION), minute, description

---

## API REST — endpoints

### Públicos (sin autenticación)
```
POST   /api/auth/register
POST   /api/auth/login
GET    /api/matches
GET    /api/bars/nearby?lat={lat}&lng={lng}
GET    /api/bars/{id}
GET    /api/bars/{id}/reviews
```

### Rol USER
```
POST   /api/bars/{id}/reviews
POST   /api/users/me/favorites/{barId}
DELETE /api/users/me/favorites/{barId}
GET    /api/users/me/favorites
```

### Rol BAR (dueño de bar)
```
GET    /api/bars/me
POST   /api/bars/me          (crear o actualizar su bar)
POST   /api/matches/{id}/schedule   (programar retransmisión)
```

### Rol ADMIN
```
GET    /api/admin/bars/pending
PATCH  /api/admin/bars/{id}/approve
```

### Docs interactivas
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Panel admin: `http://localhost:8080/admin.html`

---

## Seguridad

- **JWT stateless**: todos los endpoints protegidos requieren `Authorization: Bearer <token>`
- Token expira en **1 hora** (configurable con `matchbar.jwt.expiration-ms`)
- Roles: `USER`, `BAR`, `ADMIN` — cada controller valida con `@PreAuthorize` o security config
- Contraseñas hasheadas con BCrypt

---

## Configuración y variables de entorno

```yaml
# application.yml — valores por defecto (desarrollo)
MONGODB_URI=mongodb://localhost:27017/matchbar
JWT_SECRET=cambia-esta-clave-en-produccion-debe-ser-larga-min-256-bits-1234567890
```

En producción (Docker): pasar via environment en `docker-compose.yml`.

---

## Cómo arrancar

### Backend (desarrollo rápido)
```bash
cd backend
./mvnw spring-boot:run
# API en http://localhost:8080
```

### Backend + MongoDB (Docker)
```bash
docker compose up --build
```

### Android
1. Abrir `android/` con Android Studio Hedgehog+
2. Backend corriendo en `localhost:8080`
3. El emulador usa `http://10.0.2.2:8080/` como base URL (localhost del host)
4. Añadir Google Maps API key en `res/values/strings.xml` (opcional — mapas en gris sin clave)
5. Run

---

## Usuarios de prueba

Todos con contraseña `password123`:

| Email | Rol | Notas |
|-------|-----|-------|
| `admin@matchbar.com` | ADMIN | Panel web y aprobación de bares |
| `mario@test.com` | USER | Búsqueda, favoritos, reseñas |
| `rincon@test.com` | BAR | Bar aprobado |
| `centenario@test.com` | BAR | Bar aprobado |
| `penalti@test.com` | BAR | Bar en estado PENDING |

---

## Convenciones de código

### Backend (Java)
- DTOs como Java `record` (inmutables)
- Validación con Bean Validation (`@NotBlank`, `@Email`, etc.) en los request DTOs
- Excepciones de negocio lanzadas como `ApiException(HttpStatus, mensaje)`, capturadas por `GlobalExceptionHandler`
- Repositorios son interfaces Spring Data MongoDB — no implementar nada manualmente
- Servicios contienen toda la lógica de negocio; controllers solo delegan
- Lombok activo: usar `@RequiredArgsConstructor` en servicios para inyección

### Android (Kotlin)
- Patrón MVVM: `Screen` (Composable) → `ViewModel` → `Repository` → `Api`
- Estado del UI en `StateFlow` / `MutableStateFlow` dentro del ViewModel
- `SessionStore` gestiona el token JWT y los datos del usuario en DataStore
- Serialización de JSON con `@Serializable` (Kotlinx), no Gson ni Moshi
- Navegación con Compose Navigation; rutas definidas en `Routes.kt`

---

## Tareas pendientes conocidas

- [ ] Subida real de imágenes (logo, fotos, licencia)
- [ ] Notificaciones push (Firebase Cloud Messaging)
- [ ] Tests de integración backend
- [ ] Tests de UI con Compose Testing
- [ ] CI/CD con GitHub Actions
- [ ] Refresh tokens (actualmente solo access token)

---

## Sub-agentes disponibles

Para tareas especializadas, Claude Code puede delegar en sub-agentes:

- `.claude/agents/backend.md` — experto en el backend Spring Boot / MongoDB
- `.claude/agents/android.md` — experto en la app Android Kotlin / Compose
