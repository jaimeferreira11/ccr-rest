#!/bin/bash
#
# Script para probar la generacion de reportes Insights sin frontend.
# Uso:
#   ./scripts/test-generar-reporte.sh <usuario> <contrasena> <codCliente> <codCategoria> <tipoReporte> <csvDatos> [csvFiltros]
#
# Ejemplos:
#   # Reporte NORMAL con filtro
#   ./scripts/test-generar-reporte.sh admin admin123 AJVIERCI ARROZ NORMAL \
#     ~/Downloads/Arroz_2024_datos.csv ~/Downloads/A.J.\ Vierci_Arroz_filtro.csv
#
#   # Reporte CADENA sin filtro (usa filtros_base del cliente o fallback)
#   ./scripts/test-generar-reporte.sh admin admin123 BIMBO PANIFICADOS CADENA \
#     ~/Downloads/Panificados_datos.csv
#
# Requiere: curl, jq (opcional, para pretty-print)
#

set -e

BASE_URL="${CCR_BASE_URL:-http://localhost:8080/ccr-rest-api}"

# --- Validar parametros ---
if [ $# -lt 6 ]; then
    echo "Uso: $0 <usuario> <contrasena> <codCliente> <codCategoria> <tipoReporte> <csvDatos> [csvFiltros]"
    echo ""
    echo "  tipoReporte: NORMAL | CADENA"
    echo "  csvFiltros:  opcional, si no se envia se usa el filtro base del cliente"
    echo ""
    echo "Variable de entorno CCR_BASE_URL para cambiar el host (default: $BASE_URL)"
    exit 1
fi

USUARIO="$1"
CONTRASENA="$2"
COD_CLIENTE="$3"
COD_CATEGORIA="$4"
TIPO_REPORTE="$5"
CSV_DATOS="$6"
CSV_FILTROS="${7:-}"

if [ ! -f "$CSV_DATOS" ]; then
    echo "ERROR: archivo de datos no encontrado: $CSV_DATOS"
    exit 1
fi

if [ -n "$CSV_FILTROS" ] && [ ! -f "$CSV_FILTROS" ]; then
    echo "ERROR: archivo de filtros no encontrado: $CSV_FILTROS"
    exit 1
fi

# --- Funciones ---
pretty_json() {
    if command -v jq &>/dev/null; then
        jq .
    else
        cat
    fi
}

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
    echo "$LOGIN_BODY" | pretty_json
    exit 1
fi

TOKEN=$(echo "$LOGIN_BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])" 2>/dev/null \
    || echo "$LOGIN_BODY" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo "  ERROR: no se pudo extraer el token"
    echo "$LOGIN_BODY" | pretty_json
    exit 1
fi

echo "  OK - Token obtenido (${#TOKEN} chars)"

# --- 2. Generar reporte ---
echo ""
echo "=== GENERAR REPORTE ==="
echo "  POST $BASE_URL/insights/api/v1/reportes/generar"
echo "  Cliente:   $COD_CLIENTE"
echo "  Categoria: $COD_CATEGORIA"
echo "  Tipo:      $TIPO_REPORTE"
echo "  Datos:     $CSV_DATOS ($(wc -c < "$CSV_DATOS" | tr -d ' ') bytes)"

CURL_ARGS=(
    -s -w "\n%{http_code}"
    -X POST "$BASE_URL/insights/api/v1/reportes/generar"
    -H "Authorization: Bearer $TOKEN"
    -F "csvData=@$CSV_DATOS"
    -F "codCliente=$COD_CLIENTE"
    -F "codCategoria=$COD_CATEGORIA"
    -F "tipoReporte=$TIPO_REPORTE"
)

if [ -n "$CSV_FILTROS" ]; then
    echo "  Filtros:   $CSV_FILTROS ($(wc -c < "$CSV_FILTROS" | tr -d ' ') bytes)"
    CURL_ARGS+=(-F "csvFiltros=@$CSV_FILTROS")
fi

GEN_RESPONSE=$(curl "${CURL_ARGS[@]}")
HTTP_CODE=$(echo "$GEN_RESPONSE" | tail -1)
GEN_BODY=$(echo "$GEN_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" != "202" ]; then
    echo "  ERROR: generacion fallo (HTTP $HTTP_CODE)"
    echo "$GEN_BODY" | pretty_json
    exit 1
fi

INFORME_ID=$(echo "$GEN_BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])" 2>/dev/null \
    || echo "$GEN_BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

echo "  OK - Informe id=$INFORME_ID (estado: PROCESANDO)"
echo "$GEN_BODY" | pretty_json

# --- 3. Polling hasta completado ---
echo ""
echo "=== ESPERANDO RESULTADO ==="

MAX_INTENTOS=50
INTERVALO=20

for i in $(seq 1 $MAX_INTENTOS); do
    sleep $INTERVALO

    POLL_RESPONSE=$(curl -s -w "\n%{http_code}" \
        -X GET "$BASE_URL/insights/api/v1/reportes/$INFORME_ID" \
        -H "Authorization: Bearer $TOKEN")

    HTTP_CODE=$(echo "$POLL_RESPONSE" | tail -1)
    POLL_BODY=$(echo "$POLL_RESPONSE" | sed '$d')

    ESTADO=$(echo "$POLL_BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['estado'])" 2>/dev/null \
        || echo "$POLL_BODY" | grep -o '"estado":"[^"]*"' | cut -d'"' -f4)

    echo "  [$i/${MAX_INTENTOS}] estado=$ESTADO (${i}x${INTERVALO}s)"

    if [ "$ESTADO" = "COMPLETADO" ]; then
        echo ""
        echo "=== COMPLETADO ==="
        echo "$POLL_BODY" | pretty_json

        NOMBRE_ARCHIVO=$(echo "$POLL_BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['nombreArchivo'])" 2>/dev/null \
            || echo "$POLL_BODY" | grep -o '"nombreArchivo":"[^"]*"' | cut -d'"' -f4)

        # Descargar
        DESTINO="${NOMBRE_ARCHIVO:-informe_${INFORME_ID}.xlsx}"
        echo ""
        echo "=== DESCARGANDO ==="
        echo "  GET $BASE_URL/insights/api/v1/reportes/$INFORME_ID/descargar"
        echo "  Destino: ./$DESTINO"

        curl -s -o "$DESTINO" \
            -X GET "$BASE_URL/insights/api/v1/reportes/$INFORME_ID/descargar" \
            -H "Authorization: Bearer $TOKEN"

        FILE_SIZE=$(wc -c < "$DESTINO" | tr -d ' ')
        echo "  OK - $DESTINO ($FILE_SIZE bytes)"
        echo ""
        echo "Listo. Abri $DESTINO en Excel para verificar las pivot tables."
        exit 0
    fi

    if [ "$ESTADO" = "ERROR" ]; then
        echo ""
        echo "=== ERROR ==="
        echo "$POLL_BODY" | pretty_json
        exit 1
    fi
done

echo "  TIMEOUT: el informe no se completo en $((MAX_INTENTOS * INTERVALO)) segundos."
exit 1
