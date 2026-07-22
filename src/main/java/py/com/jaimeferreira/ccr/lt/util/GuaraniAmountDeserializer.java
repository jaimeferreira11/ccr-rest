package py.com.jaimeferreira.ccr.lt.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Deserializador de montos de dinero en guaraníes (Gs) para la ingesta de LT.
 *
 * A diferencia de {@link TolerantBigDecimalDeserializer} (usado para unidades, donde
 * los decimales SON válidos), el guaraní no usa centavos: un punto o coma en un monto
 * es, casi siempre, un separador de MILES. Por eso aquí un separador se interpreta como
 * decimal SOLO cuando es el último y va seguido de 1 o 2 dígitos (posible parte decimal
 * residual); si va seguido de 3 dígitos es un grupo de miles.
 *
 *   "1.234"      -> 1234       (punto de miles, NO 1.234)
 *   "12.500"     -> 12500
 *   "1.234.567"  -> 1234567
 *   "1.234,50"   -> 1234.50    (coma decimal residual, se conserva)
 *   "1234,5"     -> 1234.5
 *
 * Ante un valor que no se puede interpretar, loguea el crudo y devuelve {@code null}
 * (la columna es nullable), sin abortar el resto del registro ni del lote.
 */
public class GuaraniAmountDeserializer extends JsonDeserializer<BigDecimal> {

    private static final Logger LOGGER = LoggerFactory.getLogger("lt.audit");

    @Override
    public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken token = p.getCurrentToken();

        if (token == JsonToken.VALUE_NUMBER_FLOAT || token == JsonToken.VALUE_NUMBER_INT) {
            return p.getDecimalValue();
        }

        if (token == JsonToken.VALUE_STRING) {
            String raw = p.getText();
            BigDecimal parsed = parseGuarani(raw);
            if (parsed == null && raw != null && !raw.trim().isEmpty()) {
                LOGGER.warn("Monto Gs no parseable en ticket LT | campo={} | valor_crudo=\"{}\" | se guarda NULL",
                            p.getCurrentName(), raw);
            }
            return parsed;
        }

        return null;
    }

    /**
     * Normaliza un monto en guaraníes en formato ambiguo a {@link BigDecimal}.
     * Los separadores se tratan como de miles salvo un decimal residual de 1-2 dígitos.
     * Devuelve {@code null} si el valor está vacío o no se puede interpretar.
     */
    private static BigDecimal parseGuarani(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim().replaceAll("[^0-9,.-]", "");
        if (s.isEmpty() || "-".equals(s)) {
            return null;
        }

        int lastSep = Math.max(s.lastIndexOf('.'), s.lastIndexOf(','));

        String normalized;
        if (lastSep < 0) {
            normalized = s;                                        // entero puro
        } else {
            String after = s.substring(lastSep + 1);
            if (after.length() >= 1 && after.length() <= 2) {
                // parte decimal residual: se conserva; los separadores previos son de miles
                String integerPart = s.substring(0, lastSep).replaceAll("[.,]", "");
                normalized = integerPart + "." + after;
            } else {
                // solo separadores de miles
                normalized = s.replaceAll("[.,]", "");
            }
        }

        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
