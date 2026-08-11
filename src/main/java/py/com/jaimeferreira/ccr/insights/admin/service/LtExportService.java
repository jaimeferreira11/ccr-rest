package py.com.jaimeferreira.ccr.insights.admin.service;

import com.opencsv.CSVWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import py.com.jaimeferreira.ccr.commons.util.CsvNumberFormatter;
import py.com.jaimeferreira.ccr.lt.entity.LtPersona;
import py.com.jaimeferreira.ccr.lt.entity.LtProducto;
import py.com.jaimeferreira.ccr.lt.entity.LtSucursal;
import py.com.jaimeferreira.ccr.lt.entity.LtTicket;
import py.com.jaimeferreira.ccr.lt.repository.LtPersonaRepository;
import py.com.jaimeferreira.ccr.lt.repository.LtProductoRepository;
import py.com.jaimeferreira.ccr.lt.repository.LtSucursalRepository;
import py.com.jaimeferreira.ccr.lt.repository.LtTicketRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Genera exports en CSV de las tablas del módulo lt para uso administrativo.
 *
 * @author Jaime Ferreira
 */
@Service
public class LtExportService {

    private static final char CSV_SEPARATOR = ';';
    private static final byte[] UTF8_BOM = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
    private static final DateTimeFormatter FECHA_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HORA_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final String[] TICKETS_HEADER = {
            "id", "punto", "nro_ticket", "fecha", "hora", "eancode", "ean_desc",
            "unidades_vendidas", "precio_regular", "precio_promocional", "tipo_venta", "estado"
    };

    private static final String[] PRODUCTOS_HEADER = {
            "id", "eancode", "descripcion", "id_sector", "sector", "id_seccion", "seccion",
            "id_categoria", "categoria", "id_subcategoria", "subcategoria", "fabricante", "marca",
            "contenido", "pesovolumen", "unidad_medida", "estado"
    };

    private static final String[] PERSONAS_HEADER = {
            "id", "punto", "nro_ticket", "fecha", "identificacion", "nombre_y_apellido_empresa", "estado"
    };

    private static final String[] SUCURSALES_HEADER = {
            "id", "punto", "direccion", "provincia", "ciudad", "mts2", "estado"
    };

    @Autowired
    private LtTicketRepository ltTicketRepository;

    @Autowired
    private LtProductoRepository ltProductoRepository;

    @Autowired
    private LtPersonaRepository ltPersonaRepository;

    @Autowired
    private LtSucursalRepository ltSucursalRepository;

    public byte[] exportarTickets(LocalDate desde, LocalDate hasta) {
        List<LtTicket> tickets = ltTicketRepository.findByFechaBetweenOrderByFechaAscPuntoAscNroTicketAsc(desde, hasta);
        return generarCsv(TICKETS_HEADER, tickets, this::fila);
    }

    public byte[] exportarProductos() {
        List<LtProducto> productos = ltProductoRepository.findAll(Sort.by(Sort.Direction.ASC, "eancode"));
        return generarCsv(PRODUCTOS_HEADER, productos, this::fila);
    }

    public byte[] exportarPersonas() {
        List<LtPersona> personas = ltPersonaRepository.findAll(
                Sort.by(Sort.Direction.ASC, "fecha", "punto", "nroTicket"));
        return generarCsv(PERSONAS_HEADER, personas, this::fila);
    }

    public byte[] exportarSucursales() {
        List<LtSucursal> sucursales = ltSucursalRepository.findAll(Sort.by(Sort.Direction.ASC, "punto"));
        return generarCsv(SUCURSALES_HEADER, sucursales, this::fila);
    }

    private <T> byte[] generarCsv(String[] header, List<T> filas, java.util.function.Function<T, String[]> mapeador) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            buffer.write(UTF8_BOM);
            Writer writer = new OutputStreamWriter(buffer, StandardCharsets.UTF_8);
            CSVWriter csvWriter = new CSVWriter(writer, CSV_SEPARATOR,
                    CSVWriter.DEFAULT_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);

            csvWriter.writeNext(header);
            for (T fila : filas) {
                csvWriter.writeNext(mapeador.apply(fila));
            }
            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            throw new RuntimeException("Error al generar el CSV.", e);
        }
        return buffer.toByteArray();
    }

    private String[] fila(LtTicket t) {
        return new String[] {
                String.valueOf(t.getId()),
                String.valueOf(t.getPunto()),
                nullToEmpty(t.getNroTicket()),
                t.getFecha() != null ? t.getFecha().format(FECHA_FORMATTER) : "",
                t.getHora() != null ? t.getHora().format(HORA_FORMATTER) : "",
                nullToEmpty(t.getEancode()),
                sanitizarTexto(t.getEanDesc()),
                CsvNumberFormatter.formatDecimal(t.getUnidadesVendidas()),
                CsvNumberFormatter.formatDecimal(t.getPrecioRegular()),
                CsvNumberFormatter.formatDecimal(t.getPrecioPromocional()),
                sanitizarTexto(t.getTipoVenta()),
                sanitizarTexto(t.getEstado())
        };
    }

    private String[] fila(LtProducto p) {
        return new String[] {
                String.valueOf(p.getId()),
                nullToEmpty(p.getEancode()),
                sanitizarTexto(p.getDescripcion()),
                valorEntero(p.getIdSector()),
                sanitizarTexto(p.getSector()),
                valorEntero(p.getIdSeccion()),
                sanitizarTexto(p.getSeccion()),
                valorEntero(p.getIdCategoria()),
                sanitizarTexto(p.getCategoria()),
                valorEntero(p.getIdSubcategoria()),
                sanitizarTexto(p.getSubcategoria()),
                sanitizarTexto(p.getFabricante()),
                sanitizarTexto(p.getMarca()),
                CsvNumberFormatter.formatDecimal(p.getContenido()),
                CsvNumberFormatter.formatDecimal(p.getPesovolumen()),
                sanitizarTexto(p.getUnidadMedida()),
                sanitizarTexto(p.getEstado())
        };
    }

    private String[] fila(LtPersona p) {
        return new String[] {
                String.valueOf(p.getId()),
                String.valueOf(p.getPunto()),
                nullToEmpty(p.getNroTicket()),
                p.getFecha() != null ? p.getFecha().format(FECHA_FORMATTER) : "",
                sanitizarTexto(p.getIdentificacion()),
                sanitizarTexto(p.getNombreYApellidoEmpresa()),
                sanitizarTexto(p.getEstado())
        };
    }

    private String[] fila(LtSucursal s) {
        return new String[] {
                String.valueOf(s.getId()),
                String.valueOf(s.getPunto()),
                sanitizarTexto(s.getDireccion()),
                sanitizarTexto(s.getProvincia()),
                sanitizarTexto(s.getCiudad()),
                CsvNumberFormatter.formatDecimal(s.getMts2()),
                sanitizarTexto(s.getEstado())
        };
    }

    private String valorEntero(Integer valor) {
        return valor != null ? String.valueOf(valor) : "";
    }

    private String nullToEmpty(String valor) {
        return valor != null ? valor : "";
    }

    /**
     * Neutraliza inyección de fórmulas CSV (valores que arrancan con =, +, -, @) en campos de
     * texto libre que vienen de datos externos (proveedor LT) sin validar.
     */
    private String sanitizarTexto(String valor) {
        if (valor == null || valor.isEmpty()) {
            return "";
        }
        char primero = valor.charAt(0);
        if (primero == '=' || primero == '+' || primero == '-' || primero == '@') {
            return "'" + valor;
        }
        return valor;
    }
}
