#!/bin/bash
#
# Script para probar los endpoints de la app mobile SCJ (Johnson).
# Uso:
#   ./scripts/test-jhonson-app.sh <usuario> <contrasena>
#
# Ejemplo:
#   ./scripts/test-jhonson-app.sh 4800301 123456
#
# Requiere: curl, jq (opcional, para pretty-print)
#

set -e

BASE_URL="${CCR_BASE_URL:-http://192.168.100.38:8081/ccr-rest-api}"
API="$BASE_URL/jhonson/api/v1"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

PASS=0
FAIL=0

# --- Validar parametros ---
if [ $# -lt 2 ]; then
    echo "Uso: $0 <usuario> <contrasena>"
    echo ""
    echo "Variable de entorno CCR_BASE_URL para cambiar el host (default: $BASE_URL)"
    exit 1
fi

USUARIO="$1"
PASSWORD="$2"

check_result() {
    local test_name="$1"
    local expected_code="$2"
    local actual_code="$3"
    local body="$4"

    if [ "$actual_code" = "$expected_code" ]; then
        echo -e "  ${GREEN}PASS${NC} [$actual_code] $test_name"
        PASS=$((PASS + 1))
    else
        echo -e "  ${RED}FAIL${NC} [$actual_code] $test_name (esperado: $expected_code)"
        if [ -n "$body" ]; then
            echo "        Body: $(echo "$body" | head -c 200)"
        fi
        FAIL=$((FAIL + 1))
    fi
}

# --- 0. Login ---
echo ""
echo -e "${YELLOW}=== LOGIN ===${NC}"

LOGIN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"usuario\":\"$USUARIO\",\"contrasena\":\"$PASSWORD\",\"codCliente\":\"SCJ\"}")

LOGIN_BODY=$(echo "$LOGIN_RESPONSE" | sed '$d')
LOGIN_CODE=$(echo "$LOGIN_RESPONSE" | tail -n 1)

check_result "POST /auth/login" "200" "$LOGIN_CODE" "$LOGIN_BODY"

if [ "$LOGIN_CODE" != "200" ]; then
    echo -e "${RED}No se pudo hacer login. Abortando.${NC}"
    exit 1
fi

TOKEN=$(echo "$LOGIN_BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])" 2>/dev/null || echo "$LOGIN_BODY" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo -e "${RED}No se pudo extraer el token. Abortando.${NC}"
    exit 1
fi

echo "  Token obtenido: ${TOKEN:0:30}..."
AUTH="Authorization: Bearer $TOKEN"

# --- 1. GET /bocas (mes actual) ---
echo ""
echo -e "${YELLOW}=== BOCAS ===${NC}"

RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$API/bocas" -H "$AUTH")
BODY=$(echo "$RESPONSE" | sed '$d')
CODE=$(echo "$RESPONSE" | tail -n 1)
COUNT=$(echo "$BODY" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))" 2>/dev/null || echo "?")
check_result "GET /bocas (mes actual) — $COUNT registros" "200" "$CODE"

# --- 2. GET /bocas/all ---
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$API/bocas/all" -H "$AUTH")
BODY=$(echo "$RESPONSE" | sed '$d')
CODE=$(echo "$RESPONSE" | tail -n 1)
COUNT=$(echo "$BODY" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))" 2>/dev/null || echo "?")
check_result "GET /bocas/all — $COUNT registros" "200" "$CODE"

# --- 3. GET /bocas/usuario ---
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$API/bocas/usuario" -H "$AUTH")
BODY=$(echo "$RESPONSE" | sed '$d')
CODE=$(echo "$RESPONSE" | tail -n 1)
COUNT=$(echo "$BODY" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))" 2>/dev/null || echo "?")
check_result "GET /bocas/usuario — $COUNT registros" "200" "$CODE"

# --- 4. GET /cabeceras ---
echo ""
echo -e "${YELLOW}=== CABECERAS ===${NC}"

RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$API/cabeceras" -H "$AUTH")
BODY=$(echo "$RESPONSE" | sed '$d')
CODE=$(echo "$RESPONSE" | tail -n 1)
COUNT=$(echo "$BODY" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))" 2>/dev/null || echo "?")
check_result "GET /cabeceras — $COUNT registros" "200" "$CODE"

# --- 5. GET /items ---
echo ""
echo -e "${YELLOW}=== ITEMS ===${NC}"

RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$API/items" -H "$AUTH")
BODY=$(echo "$RESPONSE" | sed '$d')
CODE=$(echo "$RESPONSE" | tail -n 1)
COUNT=$(echo "$BODY" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))" 2>/dev/null || echo "?")
check_result "GET /items — $COUNT registros" "200" "$CODE"

# --- 6. POST /respuestas ---
echo ""
echo -e "${YELLOW}=== RESPUESTAS ===${NC}"

# Obtener primera boca para usar en la prueba
BOCA_ID=$(echo "$BODY" | python3 -c "
import sys,json
# Usamos los datos de /bocas/all para obtener una boca
" 2>/dev/null || echo "")

# Obtener una boca valida
BOCAS_RESPONSE=$(curl -s -X GET "$API/bocas/all" -H "$AUTH")
BOCA_JSON=$(echo "$BOCAS_RESPONSE" | python3 -c "
import sys,json
bocas = json.load(sys.stdin)
if bocas:
    b = bocas[0]
    print(json.dumps({'id': b['id'], 'codBoca': b['codBoca'], 'nombre': b['nombre'], 'canalCcr': b.get('canalCcr','AUTOSERVICIO')}))
else:
    print('{}')
" 2>/dev/null || echo "{}")

BOCA_ID=$(echo "$BOCA_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))" 2>/dev/null || echo "")
BOCA_COD=$(echo "$BOCA_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin).get('codBoca',''))" 2>/dev/null || echo "")
BOCA_NOMBRE=$(echo "$BOCA_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin).get('nombre',''))" 2>/dev/null || echo "")
BOCA_CANAL=$(echo "$BOCA_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin).get('canalCcr','AUTOSERVICIO'))" 2>/dev/null || echo "AUTOSERVICIO")

if [ -n "$BOCA_ID" ]; then
    RESPUESTA_BODY="[{
        \"idBoca\": $BOCA_ID,
        \"codBoca\": \"$BOCA_COD\",
        \"descBoca\": \"$BOCA_NOMBRE\",
        \"canalCcr\": \"$BOCA_CANAL\",
        \"usuario\": \"$USUARIO\",
        \"longitud\": \"-57.6\",
        \"latitud\": \"-25.3\",
        \"fechaCreacion\": \"2026-04-24\",
        \"horaInicio\": \"10:00\",
        \"horaFin\": \"10:30\",
        \"detalles\": [{
            \"idItem\": 1,
            \"descItem\": \"Item test\",
            \"codCabecera\": \"FI\",
            \"valor1\": \"SI\",
            \"sinDatos\": false
        }],
        \"imagenes\": []
    }]"

    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API/respuestas" \
        -H "$AUTH" -H "Content-Type: application/json" \
        -d "$RESPUESTA_BODY")
    BODY=$(echo "$RESPONSE" | sed '$d')
    CODE=$(echo "$RESPONSE" | tail -n 1)
    check_result "POST /respuestas (boca $BOCA_COD)" "201" "$CODE" "$BODY"
else
    echo -e "  ${YELLOW}SKIP${NC} POST /respuestas — no hay bocas disponibles"
fi

# --- 7. GET /respuestas ---
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$API/respuestas" -H "$AUTH")
BODY=$(echo "$RESPONSE" | sed '$d')
CODE=$(echo "$RESPONSE" | tail -n 1)
COUNT=$(echo "$BODY" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))" 2>/dev/null || echo "?")
check_result "GET /respuestas — $COUNT registros" "200" "$CODE"

# --- 8. PUT /usuarios/change-password (caso error: password incorrecto) ---
echo ""
echo -e "${YELLOW}=== USUARIOS ===${NC}"

RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$API/usuarios/change-password" \
    -H "$AUTH" -H "Content-Type: application/json" \
    -d "{\"usuario\":\"$USUARIO\",\"oldPassword\":\"password_incorrecto\",\"newPassword\":\"nueva123\"}")
BODY=$(echo "$RESPONSE" | sed '$d')
CODE=$(echo "$RESPONSE" | tail -n 1)
check_result "PUT /change-password (password incorrecto → 403)" "403" "$CODE"

# --- Resumen ---
echo ""
echo -e "${YELLOW}=== RESUMEN ===${NC}"
TOTAL=$((PASS + FAIL))
echo -e "  Total: $TOTAL | ${GREEN}PASS: $PASS${NC} | ${RED}FAIL: $FAIL${NC}"

if [ $FAIL -gt 0 ]; then
    exit 1
fi
