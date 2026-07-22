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
 * Deserializador tolerante de {@link BigDecimal} para la ingesta de LT.
 *
 * LT envía los decimales en formatos inconsistentes (coma decimal, punto decimal,
 * punto como separador de miles, o número JSON nativo). En vez de abortar todo el
 * lote cuando un valor viene mal formateado, este deserializador:
 *   - acepta números JSON nativos tal cual,
 *   - normaliza strings aplicando una heurística de separadores,
 *   - ante un valor que no se puede parsear, loguea el crudo y devuelve {@code null}
 *     (la columna es nullable), permitiendo que el resto del registro y del lote
 *     se procese normalmente.
 *
 * Heurística de separadores (para strings):
 *   - Punto y coma presentes: el ÚLTIMO que aparece es el separador decimal;
 *     el otro se trata como separador de miles y se elimina.
 *       "1.234,50"  -> 1234.50      "1,234.50" -> 1234.50
 *   - Solo comas: una coma = decimal ("0,450" -> 0.450);
 *     varias comas = miles ("1,234,567" -> 1234567).
 *   - Solo puntos: un punto = decimal ("0.45" -> 0.45);
 *     varios puntos = miles ("1.234.567" -> 1234567).
 */
public class TolerantBigDecimalDeserializer extends JsonDeserializer<BigDecimal> {

    private static final Logger LOGGER = LoggerFactory.getLogger("lt.audit");

    @Override
    public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken token = p.getCurrentToken();

        // Número JSON nativo (0.450, 12): se usa directamente.
        if (token == JsonToken.VALUE_NUMBER_FLOAT || token == JsonToken.VALUE_NUMBER_INT) {
            return p.getDecimalValue();
        }

        // String: se normaliza con la heurística de separadores.
        if (token == JsonToken.VALUE_STRING) {
            String raw = p.getText();
            BigDecimal parsed = parseFlexible(raw);
            if (parsed == null && raw != null && !raw.trim().isEmpty()) {
                LOGGER.warn("Valor decimal no parseable en ticket LT | campo={} | valor_crudo=\"{}\" | se guarda NULL",
                            p.getCurrentName(), raw);
            }
            return parsed;
        }

        // null u otro token: sin valor.
        return null;
    }

    /**
     * Normaliza un string numérico en formato ambiguo a {@link BigDecimal}.
     * Devuelve {@code null} si el valor está vacío o no se puede interpretar.
     */
    private static BigDecimal parseFlexible(String raw) {
        if (raw == null) {
            return null;
        }
        // Deja solo dígitos, separadores y signo (quita espacios, símbolos de moneda, etc.).
        String s = raw.trim().replaceAll("[^0-9,.-]", "");
        if (s.isEmpty() || "-".equals(s)) {
            return null;
        }

        int lastDot = s.lastIndexOf('.');
        int lastComma = s.lastIndexOf(',');

        if (lastDot >= 0 && lastComma >= 0) {
            if (lastComma > lastDot) {
                // coma decimal, punto de miles
                s = s.replace(".", "").replace(',', '.');
            } else {
                // punto decimal, coma de miles
                s = s.replace(",", "");
            }
        } else if (lastComma >= 0) {
            s = (countChar(s, ',') > 1)
                    ? s.replace(",", "")      // varias comas = miles
                    : s.replace(',', '.');    // una coma = decimal
        } else if (lastDot >= 0 && countChar(s, '.') > 1) {
            s = s.replace(".", "");           // varios puntos = miles
        }
        // (un solo punto se deja como decimal)

        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int countChar(String s, char c) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }
}
