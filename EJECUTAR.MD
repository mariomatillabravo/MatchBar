# Cómo ejecutar MatchBar

## 1. Configuración (solo la primera vez)

```bash
cp .env.example .env
```

Edita `.env` y rellena como mínimo `MONGO_ROOT_PASSWORD`, `MONGO_APP_PASSWORD` y `JWT_SECRET`:

```bash
openssl rand -hex 24      # para cada contraseña de Mongo
openssl rand -base64 48   # para JWT_SECRET
```

Para desarrollar en tu máquina con los datos de prueba pon `SPRING_PROFILES_ACTIVE=dev`
(usuarios con contraseña `password123`). **Nunca** uses `dev` en un servidor.

> Si ya habías arrancado la versión anterior (Mongo sin contraseña), borra una vez el
> volumen con `docker compose down -v`: el antiguo no tiene usuarios y la API no conectará.

## 2. Arrancar con Docker

```bash
docker compose up --build   # primera vez o tras cambiar el backend
docker compose up           # siguientes veces
```

Reiniciar desde cero (borra la base de datos):

```bash
docker compose down -v --rmi local
docker compose build --no-cache
docker compose up
```

## Backend con Maven y Mongo en Docker

```bash
docker compose up -d mongo
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev     # en PowerShell: .\mvnw.cmd ...
```

El perfil `dev` lee el `.env` de la raíz, así que usa las mismas credenciales de Mongo.
Solo necesitas el JDK 21: el wrapper descarga y verifica Maven automáticamente.

Tests del backend (no necesitan MongoDB): `./mvnw verify`.

## URLs

- Panel admin: http://localhost:8080/admin.html
- Swagger: http://localhost:8080/swagger-ui.html (solo con el perfil `dev` o `SWAGGER_ENABLED=true`)

## App Android

- **Debug**: apunta a `http://10.0.2.2:8080/` (el localhost del PC visto desde el emulador).
  Para un móvil físico: `API_BIND=0.0.0.0` en `.env`, tu IP en `app/build.gradle.kts`
  (bloque `debug`) y en `app/src/debug/res/xml/network_security_config.xml`.
- **Release**: exige una URL HTTPS:
  `./gradlew assembleRelease -Pmatchbar.releaseApiUrl=https://api.tudominio.com/`

## Servidor con HTTPS

Con un dominio que apunte al servidor y los puertos 80/443 abiertos, pon
`MATCHBAR_DOMAIN=api.tudominio.com` en `.env` y arranca con el proxy Caddy:

```bash
docker compose --profile tls up -d --build
```

En producción deja `SPRING_PROFILES_ACTIVE=prod` y define `MATCHBAR_ADMIN_EMAIL` y
`MATCHBAR_ADMIN_PASSWORD` para crear el primer administrador.
