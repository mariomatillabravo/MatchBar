#!/usr/bin/env bash
#
# Mete una imagen en el bar "El Penalti" (usuario penalti@test.com) tanto en
# las fotos del establecimiento como en la carta, y aprueba el bar para que
# aparezca en la pestaña "Bares" del panel admin.
#
# Uso:
#   ./seed-penalti-image.sh /ruta/a/la/imagen.jpg
#   API_BASE=http://localhost:8080 ./seed-penalti-image.sh ~/penalti.jpg
#
# Requisitos: el backend debe estar levantado y curl + python3 disponibles.
# La imagen debe ser JPG, PNG o WEBP (lo que admite el backend).

set -euo pipefail

IMG="${1:-}"
API_BASE="${API_BASE:-http://localhost:8080}"

if [[ -z "$IMG" || ! -f "$IMG" ]]; then
  echo "❌ Pásame la ruta a una imagen existente. Ej: ./seed-penalti-image.sh ~/penalti.jpg" >&2
  exit 1
fi

# Tipo MIME según la extensión (el backend valida que sea jpg/png/webp)
case "${IMG,,}" in
  *.png)         CT="image/png" ;;
  *.webp)        CT="image/webp" ;;
  *.jpg|*.jpeg)  CT="image/jpeg" ;;
  *)             CT="image/jpeg" ;;
esac

# Extrae un campo de un JSON por stdin (sin depender de jq)
json_field() { python3 -c "import sys,json;print(json.load(sys.stdin).get('$1',''))"; }

echo "→ Login como el bar (penalti@test.com)…"
BAR_TOKEN=$(curl -fsS -X POST "$API_BASE/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"penalti@test.com","password":"password123"}' | json_field token)
[[ -n "$BAR_TOKEN" ]] || { echo "❌ No se pudo iniciar sesión como el bar"; exit 1; }

echo "→ Subiendo la imagen a FOTOS del establecimiento…"
RESP=$(curl -fsS -X POST "$API_BASE/api/bars/me/photos" \
  -H "Authorization: Bearer $BAR_TOKEN" \
  -F "file=@${IMG};type=${CT}")
BAR_ID=$(printf '%s' "$RESP" | json_field id)
echo "   bar id = $BAR_ID"

echo "→ Subiendo la misma imagen a la CARTA del bar…"
curl -fsS -X POST "$API_BASE/api/bars/me/menu" \
  -H "Authorization: Bearer $BAR_TOKEN" \
  -F "file=@${IMG};type=${CT}" >/dev/null

echo "→ Login como admin para aprobar el bar…"
ADMIN_TOKEN=$(curl -fsS -X POST "$API_BASE/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@matchbar.com","password":"password123"}' | json_field token)

echo "→ Aprobando el bar para que aparezca en la pestaña Bares…"
curl -fsS -X PATCH "$API_BASE/api/admin/bars/${BAR_ID}/approve" \
  -H "Authorization: Bearer $ADMIN_TOKEN" >/dev/null

echo
echo "✅ Hecho. Abre $API_BASE/admin.html → pestaña 'Bares' → 'El Penalti'."
echo "   Verás la imagen tanto en 'Fotos del establecimiento' como en 'Carta del bar'."
