#!/bin/bash
#
# Smoke test: eancode pasa de BIGINT a VARCHAR en lt.producto/lt.ticket,
# sin cambiar el contrato JSON con el proveedor (sigue enviándose como número).
# Verifica:
#   - el endpoint sigue aceptando eancode numérico sin comillas (contrato intacto),
#   - el valor se persiste como texto en la columna,
#   - el upsert por eancode (buscar-y-actualizar) sigue funcionando: reenviar el
#     mismo eancode actualiza la fila existente, no crea un duplicado.
#
# Uso:
#   ./scripts/test-lt-eancode-text.sh
#
# Requiere: curl, jq (opcional), psql apuntando a la misma BD que usa la app.

set -e

BASE_URL="${CCR_BASE_URL:-http://localhost:8081/ccr-rest-api}"
API_KEY="${LT_API_KEY:-ltk_KFYQNxRAls24J46tSwndn68UYPfp9_SfyumbVCa9_es}"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}POST $BASE_URL/lt/api/v1/productos (eancode numérico, sin comillas)${NC}"
curl -sS -X POST "$BASE_URL/lt/api/v1/productos" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '[{"eancode": 7790099887766, "descripcion": "SMK eancode text", "fabricante": "Test", "marca": "Test"}]' \
  | (jq . 2>/dev/null || cat)

echo
echo -e "${YELLOW}Reenviando el MISMO eancode con descripción distinta (debe actualizar, no duplicar)${NC}"
curl -sS -X POST "$BASE_URL/lt/api/v1/productos" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '[{"eancode": 7790099887766, "descripcion": "SMK eancode text - actualizado", "fabricante": "Test", "marca": "Test"}]' \
  | (jq . 2>/dev/null || cat)

echo
echo -e "${YELLOW}POST $BASE_URL/lt/api/v1/tickets (eancode numérico, sin comillas)${NC}"
curl -sS -X POST "$BASE_URL/lt/api/v1/tickets" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '[{"punto": 5, "nroTicket": "SMK-EAN-1", "fecha": "2026-08-03", "hora": "10:00:00", "eancode": 7790099887766, "ean_desc": "SMK eancode text", "unidades_vendidas": 1, "precio_regular": "1000", "precio_promocional": "900", "tipo_venta": "P"}]' \
  | (jq . 2>/dev/null || cat)

echo
echo -e "${GREEN}Verificá en BD:${NC}"
cat <<'SQL'

  -- 1 sola fila (no duplicado), descripcion actualizada, eancode como texto
  SELECT eancode, pg_typeof(eancode), descripcion
  FROM lt.producto
  WHERE eancode = '7790099887766';

  SELECT nro_ticket, eancode, pg_typeof(eancode)
  FROM lt.ticket
  WHERE nro_ticket = 'SMK-EAN-1';

  Esperado:
    lt.producto: 1 fila, eancode = '7790099887766' (character varying), descripcion = 'SMK eancode text - actualizado'
    lt.ticket:   1 fila, eancode = '7790099887766' (character varying)
SQL
