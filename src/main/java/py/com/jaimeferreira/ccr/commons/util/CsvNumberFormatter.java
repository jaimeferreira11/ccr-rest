package py.com.jaimeferreira.ccr.commons.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Formatea valores BigDecimal para columnas de export CSV: entero plano si el valor
 * no tiene parte fraccionaria, o con coma decimal y 3 dígitos si la tiene.
 *
 * @author Jaime Ferreira
 */
public final class CsvNumberFormatter {

    private CsvNumberFormatter() {
    }

    public static String formatDecimal(BigDecimal valor) {
        if (valor == null) {
            return "";
        }
        if (valor.stripTrailingZeros().scale() <= 0) {
            return valor.toBigInteger().toString();
        }
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols(Locale.ROOT);
        simbolos.setDecimalSeparator(',');
        DecimalFormat formato = new DecimalFormat("0.000", simbolos);
        formato.setGroupingUsed(false);
        return formato.format(valor);
    }
}
