# MatchBar

Aplicación móvil para localizar bares que retransmiten partidos de fútbol en
tiempo real, con backend Spring Boot, app Android nativa (Kotlin + Jetpack
Compose) y panel de administración web.

Proyecto de fin de ciclo DAM — IES Tetuán de las Victorias (curso 2025/2026).

---

## Stack

- **Backend**: Spring Boot 3.3 + Spring Data JPA + Spring Security + JWT
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

### Opción A — Con Maven y H2 (la más rápida para desarrollo)

```bash
cd backend
./mvnw spring-boot:run
```

(Si no tienes el wrapper de Maven, ejecuta `mvn spring-boot:run`).

El backend levanta en `http://localhost:8080` con perfil `dev`. Esto significa:

- Base de datos H2 en memoria (se reinicia cada arranque).
- Datos de prueba precargados desde `data-dev.sql` (3 bares, 5 partidos, etc.).
- Consola H2 en `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:matchbar`).
- Swagger UI en `http://localhost:8080/swagger-ui.html`.
- Panel admin en `http://localhost:8080/admin.html`.

### Opción B — Con Docker (incluye MySQL real)

```bash
docker compose up --build
```

Levanta dos contenedores: la API en `:8080` y MySQL en `:3306`.

### Usuarios de prueba (perfil dev)

Todos tienen contraseña: **`password123`**

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

- Android Studio Hedgehog (2023.1) o superior
- JDK 17
- Emulador con Android 8.0 (API 26) o superior

### Pasos

1. Asegúrate de que el backend esté corriendo en tu máquina (puerto 8080).
2. Abre la carpeta `android/` con Android Studio.
3. **Importante**: la URL base de la API por defecto es `http://10.0.2.2:8080/`,
   que es la dirección de `localhost` del PC anfitrión vista desde el emulador.
   - Si pruebas en un dispositivo físico, edita `app/build.gradle.kts` y cambia
     `API_BASE_URL` por la IP de tu PC en la red local (p. ej. `http://192.168.1.50:8080/`).
4. (Opcional, para mapas): pega tu API key de Google Maps en
   `app/src/main/res/values/strings.xml`. Sin clave, la pantalla del mapa se
   verá en gris pero el resto de la app funciona.
5. Pulsa **Run**.

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
- [ ] CI con GitHub Actions.
- [ ] Refresh tokens.

---

## Equipo

- Roberto Fernández Picatoste
- Roberto Asperilla Rabadán
- Mario Matilla Bravo
