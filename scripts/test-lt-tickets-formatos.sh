#!/bin/bash
#
# Smoke test de ingesta de tickets LT con montos/unidades en formatos mixtos.
# Verifica que:
#   - unidades con coma decimal ("0,450") se parseen bien,
#   - precios en Gs con separador de miles ("1.234") se guarden como 1234,
#   - un valor basura NO tumbe todo el lote (se guarda NULL + WARN en lt.audit),
#     y los demás tickets del lote se persistan igual.
#
# Uso:
#   ./scripts/test-lt-tickets-formatos.sh
#
# Requiere: curl, jq (opcional, para pretty-print)
#

set -e

BASE_URL="${CCR_BASE_URL:-http://localhost:8081/ccr-rest-api}"
API_KEY="${LT_API_KEY:-ltk_KFYQNxRAls24J46tSwndn68UYPfp9_SfyumbVCa9_es}"
URL="$BASE_URL/lt/api/v1/tickets"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}POST $URL${NC}"

# Lote con 4 tickets: formatos mixtos + 1 valor basura en unidades.
read -r -d '' PAYLOAD <<'JSON' || true
[
  {
    "punto": 5, "nroTicket": "SMK-1001", "fecha": "2026-07-02", "hora": "10:15:00",
    "eancode": 7790001, "ean_desc": "Prod coma-decimal",
    "unidades_vendidas": "0,450", "precio_regular": "1.234", "precio_promocional": "980",
    "tipo_venta": "P"
  },
  {
    "punto": 5, "nroTicket": "SMK-1002", "fecha": "2026-07-02", "hora": "10:16:00",
    "eancode": 7790002, "ean_desc": "Prod numero nativo",
    "unidades_vendidas": 2.5, "precio_regular": "12.500", "precio_promocional": "1.234.567",
    "tipo_venta": "R"
  },
  {
    "punto": 5, "nroTicket": "SMK-1003", "fecha": "2026-07-02", "hora": "10:17:00",
    "eancode": 7790003, "ean_desc": "Prod unidad basura -> NULL, no rompe lote",
    "unidades_vendidas": "abc", "precio_regular": "5.000", "precio_promocional": null,
    "tipo_venta": "R"
  },
  {
    "punto": 5, "nroTicket": "SMK-1004", "fecha": "2026-07-02", "hora": "10:18:00",
    "eancode": 7790004, "ean_desc": "Prod punto-decimal en unidad",
    "unidades_vendidas": "1.750", "precio_regular": "999", "precio_promocional": "899",
    "tipo_venta": "P"
  }
]
JSON

curl -sS -X POST "$URL" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$PAYLOAD" | (jq . 2>/dev/null || cat)

echo
echo -e "${GREEN}Esperado:${NC} status OK, guardados=4 (el lote NO se rechaza por el valor basura)."
echo "Verificá en BD el resultado del parseo:"
cat <<'SQL'

  SELECT nro_ticket, unidades_vendidas, precio_regular, precio_promocional
  FROM lt.ticket
  WHERE nro_ticket LIKE 'SMK-%'
  ORDER BY nro_ticket;

  Valores esperados:
    SMK-1001 | 0.450 | 1234    | 980
    SMK-1002 | 2.500 | 12500   | 1234567
    SMK-1003 | NULL  | 5000    | NULL      (unidad basura -> NULL + WARN en lt.audit)
    SMK-1004 | 1.750 | 999     | 899
SQL
