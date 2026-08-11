#!/bin/bash
#
# Script para probar el export CSV de lt.ticket sin frontend.
# Uso:
#   ./scripts/test-lt-export.sh <usuario> <contrasena> <desde> <hasta>
#
# Ejemplo:
#   ./scripts/test-lt-export.sh admin admin123 2026-08-01 2026-08-04
#
# Requiere: curl
#

set -e

BASE_URL="${CCR_BASE_URL:-http://localhost:8080/ccr-rest-api}"

if [ $# -lt 4 ]; then
    echo "Uso: $0 <usuario> <contrasena> <desde:yyyy-MM-dd> <hasta:yyyy-MM-dd>"
    echo ""
    echo "Variable de entorno CCR_BASE_URL para cambiar el host (default: $BASE_URL)"
    exit 1
fi

USUARIO="$1"
CONTRASENA="$2"
DESDE="$3"
HASTA="$4"

# --- 1. Login ---
echo "=== LOGIN ==="
echo "  POST $BASE_URL/auth/login"

LOGIN_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"usuario\": \"$USUARIO\", \"contrasena\": \"$CONTRASENA\"}")

HTTP_CODE=$(echo "$LOGIN_RESPONSE" | tail -1)
LOGIN_BODY=$(echo "$LOGIN_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" != "200" ]; then
    echo "  ERROR: login fallo (HTTP $HTTP_CODE)"
    echo "$LOGIN_BODY"
    exit 1
fi

TOKEN=$(echo "$LOGIN_BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])" 2>/dev/null \
    || echo "$LOGIN_BODY" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo "  ERROR: no se pudo extraer el token"
    echo "$LOGIN_BODY"
    exit 1
fi

echo "  OK - Token obtenido (${#TOKEN} chars)"

# --- 2. Caso 400: desde > hasta ---
echo ""
echo "=== VALIDACION: desde > hasta debe dar 400 ==="
BAD_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    -X GET "$BASE_URL/insights/api/v1/admin/lt/tickets/export?desde=$HASTA&hasta=$DESDE" \
    -H "Authorization: Bearer $TOKEN")
echo "  HTTP $BAD_CODE (esperado: 400)"

# --- 3. Export ---
echo ""
echo "=== EXPORT lt.ticket ==="
echo "  GET $BASE_URL/insights/api/v1/admin/lt/tickets/export?desde=$DESDE&hasta=$HASTA"

DESTINO="lt_tickets_${DESDE}_${HASTA}.csv"

HTTP_CODE=$(curl -s -o "$DESTINO" -w "%{http_code}" \
    -X GET "$BASE_URL/insights/api/v1/admin/lt/tickets/export?desde=$DESDE&hasta=$HASTA" \
    -H "Authorization: Bearer $TOKEN")

if [ "$HTTP_CODE" != "200" ]; then
    echo "  ERROR: export fallo (HTTP $HTTP_CODE)"
    cat "$DESTINO"
    exit 1
fi

FILE_SIZE=$(wc -c < "$DESTINO" | tr -d ' ')
echo "  OK - $DESTINO ($FILE_SIZE bytes)"
echo ""
echo "  Primeras lineas:"
head -5 "$DESTINO"
echo ""
echo "Listo. Verificar en $DESTINO: BOM UTF-8, separador ';', formato de numeros."
