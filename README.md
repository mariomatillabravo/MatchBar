# MatchBar

Aplicación móvil para localizar bares que retransmiten partidos de fútbol en
tiempo real, con backend Spring Boot, app Android nativa (Kotlin + Jetpack
Compose) y panel de administración web.

Proyecto de fin de ciclo DAM — IES Tetuán de las Victorias (curso 2025/2026).

---

## Stack

- **Backend**: Spring Boot 4.1 + Spring Data MongoDB + Spring Security 7 + JWT
- **Base de datos**: H2 en memoria (perfil `dev`) / MySQL 8 (perfil `prod`)
- **App móvil**: Kotlin + Jetpack Compose + Retrofit + DataStore + Maps Compose
- **Panel admin**: SPA estática (HTML+CSS+JS) servida desde el backend
- **Build**: Maven (backend) + Gradle KTS (Android)
- **Despliegue**: Docker + docker-compose

---

## Estructura del repositorio

```
matchbar/
├── backend/                # API Spring Boot
│   ├── src/main/java/com/matchbar/
│   │   ├── config/         # SecurityConfig
│   │   ├── controller/     # Endpoints REST
│   │   ├── dto/            # request/response (records)
│   │   ├── entity/         # JPA entities
│   │   ├── exception/      # ApiException + handler global
│   │   ├── repository/     # Spring Data JPA
│   │   ├── security/       # JWT filter, provider, principal
│   │   └── service/        # Lógica de negocio
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── data-dev.sql    # Datos de prueba (H2)
│   │   └── static/admin.html
│   ├── pom.xml
│   └── Dockerfile
├── android/                # App Android
│   ├── app/src/main/java/com/matchbar/app/
│   │   ├── data/           # api / model / local
│   │   ├── ui/
│   │   │   ├── navigation/ # NavHost + rutas
│   │   │   ├── screens/    # auth / matches / map / bars / favorites / profile / bar / admin
│   │   │   ├── common/     # componentes reutilizables
│   │   │   └── theme/
│   │   └── util/
│   ├── app/build.gradle.kts
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── docker-compose.yml
└── README.md
```

---

## Cómo arrancar el backend

Guía completa en [EJECUTAR.MD](EJECUTAR.MD). En resumen:

```bash
cp .env.example .env        # rellena contraseñas y JWT_SECRET (ver comentarios)
docker compose up --build   # MongoDB 7 + API en http://localhost:8080
```

Los secretos se leen **solo** de variables de entorno (`.env`, fuera de git) y la API
no arranca sin `JWT_SECRET`. Hay dos perfiles:

- `prod` (por defecto): sin datos de prueba. El primer admin se crea con
  `MATCHBAR_ADMIN_EMAIL` / `MATCHBAR_ADMIN_PASSWORD`.
- `dev`: siembra los usuarios de prueba de la tabla siguiente. Solo para tu máquina.

Swagger UI (solo perfil `dev`) en `http://localhost:8080/swagger-ui.html` y panel admin en
`http://localhost:8080/admin.html`.

### Usuarios de prueba (solo perfil dev)

Todos tienen contraseña: **`password123`**. Se crean únicamente con
`SPRING_PROFILES_ACTIVE=dev`; en `prod` no existen.

| Email                  | Rol   | Notas                            |
|------------------------|-------|----------------------------------|
| `admin@matchbar.com`   | ADMIN | Acceso al panel web              |
| `mario@test.com`       | USER  | Para probar búsqueda y favoritos |
| `rincon@test.com`      | BAR   | Tiene ficha aprobada             |
| `centenario@test.com`  | BAR   | Tiene ficha aprobada             |
| `penalti@test.com`     | BAR   | Ficha en estado PENDING          |

### Endpoints principales

| Método | Endpoint                         | Rol     |
|--------|----------------------------------|---------|
| POST   | `/api/auth/register`             | público |
| POST   | `/api/auth/login`                | público |
| GET    | `/api/matches`                   | público |
| GET    | `/api/bars/nearby?lat=&lng=`     | público |
| GET    | `/api/bars/{id}`                 | público |
| GET    | `/api/bars/{id}/reviews`         | público |
| POST   | `/api/bars/{id}/reviews`         | USER    |
| POST   | `/api/users/me/favorites/{barId}`| USER    |
| GET    | `/api/bars/me`                   | BAR     |
| POST   | `/api/bars/me`                   | BAR     |
| POST   | `/api/matches/{id}/schedule`     | BAR     |
| GET    | `/api/admin/bars/pending`        | ADMIN   |
| PATCH  | `/api/admin/bars/{id}/approve`   | ADMIN   |

---

## Cómo arrancar la app Android

### Requisitos

- Android Studio reciente, con soporte para AGP 9.4 (Gradle 9.8, Kotlin 2.4)
- JDK 17 o superior
- Emulador o móvil con Android 8.0 (API 26) o superior. La app usa
  `targetSdk 36` (Android 16), el mínimo que exige Google Play desde el 31/08/2026.

### Pasos

1. Asegúrate de que el backend esté corriendo en tu máquina (puerto 8080).
2. Abre la carpeta `android/` con Android Studio.
3. **Importante**: en debug la URL de la API es `http://10.0.2.2:8080/`,
   que es la dirección de `localhost` del PC anfitrión vista desde el emulador.
   - Si pruebas en un dispositivo físico, cambia `API_BASE_URL` del bloque `debug`
     en `app/build.gradle.kts` por la IP de tu PC (p. ej. `http://192.168.1.50:8080/`),
     añádela a `app/src/debug/res/xml/network_security_config.xml` y pon
     `API_BIND=0.0.0.0` en `.env`.
4. Pulsa **Run**. Los mapas usan OpenStreetMap (osmdroid) y no necesitan API key.

### Release firmada (para Google Play)

1. Crea la clave de firma **una sola vez** y guárdala fuera del repositorio
   (si se pierde, no podrás actualizar la app):
   ```bash
   keytool -genkeypair -v -keystore ~/matchbar-release.jks -alias matchbar \
           -keyalg RSA -keysize 4096 -validity 10000
   ```
2. Añade a `~/.gradle/gradle.properties` (nunca al repo):
   ```properties
   matchbar.releaseApiUrl=https://api.tudominio.com/
   matchbar.keystore.path=/ruta/a/matchbar-release.jks
   matchbar.keystore.password=...
   matchbar.key.alias=matchbar
   matchbar.key.password=...
   ```
   (En CI se pueden usar las variables `MATCHBAR_KEYSTORE_PATH`,
   `MATCHBAR_KEYSTORE_PASSWORD`, `MATCHBAR_KEY_ALIAS` y `MATCHBAR_KEY_PASSWORD`.)
3. `./gradlew bundleRelease` genera el `.aab` firmado en
   `app/build/outputs/bundle/release/`. Sin la clave configurada, el build
   termina igual pero sin firmar (sirve para comprobar, no para publicar).

La release activa R8 (código reducido y ofuscado) y no incluye la sesión en
las copias de seguridad del móvil (`allowBackup="false"`).

### Flujo de uso típico

1. Pantalla de login → usa `mario@test.com` / `password123`.
2. Verás la lista de partidos próximos.
3. Pulsa un partido → se abre el mapa con los bares cercanos que lo emiten.
4. Pulsa un bar → ves la ficha, las valoraciones y puedes marcarlo favorito.
5. Cierra sesión y entra como `rincon@test.com` para ver la pantalla de bar
   con su ficha editable.
6. Entra como `admin@matchbar.com` para ver y aprobar bares pendientes.

---

## Panel de administración web

Disponible en `http://localhost:8080/admin.html`.

Inicia sesión con `admin@matchbar.com` / `password123` y verás el listado de
bares pendientes con botones para aprobar o rechazar cada uno.

---

## Próximos pasos sugeridos

- [ ] Subida real de imágenes (carta, fotos, documento de licencia).
- [ ] Notificaciones push con Firebase Cloud Messaging.
- [ ] Tests de integración del backend.
- [ ] Tests de UI con Compose Testing.
- [x] CI con GitHub Actions (backend, Android y escaneo de secretos con gitleaks).
- [ ] Refresh tokens.

---

## Equipo

- Roberto Fernández Picatoste
- Roberto Asperilla Rabadán
- Mario Matilla Bravo
