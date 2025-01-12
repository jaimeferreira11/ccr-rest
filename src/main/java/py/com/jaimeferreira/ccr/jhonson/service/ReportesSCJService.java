
package py.com.jaimeferreira.ccr.jhonson.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.ooxml.JRPptxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePptxReportConfiguration;
import py.com.jaimeferreira.ccr.commons.exception.UnknownResourceException;
import py.com.jaimeferreira.ccr.commons.util.ManejadorDeArchivos;
import py.com.jaimeferreira.ccr.jhonson.constants.ConstantsSCJ;
import py.com.jaimeferreira.ccr.jhonson.entity.ReporteSCJ;
import py.com.jaimeferreira.ccr.jhonson.repository.DistribuidoresSCJRepository;
import py.com.jaimeferreira.ccr.jhonson.repository.ReportesSCJRepository;

/**
 *
 * @author Jaime Ferreira
 */

@Component
@Service
public class ReportesSCJService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportesSCJService.class);

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    private ReportesSCJRepository repo;

    @Autowired
    private DistribuidoresSCJRepository distribuidorRepo;

    @Autowired
    private ManejadorDeArchivos manejadorDeArchivos;

    public Integer save(ReporteSCJ reporte) throws Exception {

        if (!distribuidorRepo.findByCodigo(reporte.getCodDistribuidor()).isPresent()) {
            throw new UnknownResourceException("No se encuentra el distribuidor");
        }

        reporte.getDetalles().forEach(d -> {
            List<String> imgs = d.getPathImagenes().stream()
                                 .map(img -> {

                                     if (img.contains("externo")) {
                                         String aux = manejadorDeArchivos.getDirectoryPathMainImagenes()
                                                                         .concat(img);
                                         LOGGER.info("El path de la imagen es " + aux);
                                         return aux;

                                     }
                                     else {

                                         if ("prod".equalsIgnoreCase(ConstantsSCJ.ENV_PROFILE)) {

                                             return ConstantsSCJ.URL_PROD_IMAGES.concat(d.getCodBoca()).concat("/")
                                                                                .concat(img);
                                         }

                                         String aux = manejadorDeArchivos.getDirectoryPathImagenesJhonson()
                                                                         .concat("/")
                                                                         .concat(d.getCodBoca())
                                                                         .concat("/")
                                                                         .concat(img);
                                         LOGGER.info("El path de la imagen es " + aux);
                                         return aux;
                                     }
                                 })
                                 .collect(Collectors.toList());
            d.setPathImagenes(imgs);
        });

        ReporteSCJ newReport = repo.save(reporte);

        return newReport.getId().intValue();

    }

    public List<ReporteSCJ> getLastByUser(String usuario, Integer limit) {

        return repo.findByUsuarioOrderByFechaCreacionDesc(usuario).stream()
                   .limit(limit)
                   .collect(Collectors.toList());

    }

    private JasperPrint generateJasperPrint(Integer idReporte) throws JRException, SQLException {

        InputStream input = getClass().getResourceAsStream("/py/com/jaimeferreira/ccr/commons/jasper/ReportePDV.jrxml");
        JasperReport jasperReport = JasperCompileManager.compileReport(input);
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("idReporte", idReporte);

        return JasperFillManager.fillReport(
                                            jasperReport, parametros, jdbcTemplate.getDataSource().getConnection());
    }

    public byte[] generarReporte(Integer idReporte) throws JRException, SQLException, IOException {

        JasperPrint print = generateJasperPrint(idReporte);

        return JasperExportManager.exportReportToPdf(print);
    }

    public byte[] generarReportePPT(Integer idReporte) throws JRException, SQLException {

        JasperPrint print = generateJasperPrint(idReporte);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JRPptxExporter exporter = new JRPptxExporter();

        exporter.setExporterInput(new SimpleExporterInput(print));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));

        SimplePptxReportConfiguration configuration = new SimplePptxReportConfiguration();
        exporter.setConfiguration(configuration);

        exporter.exportReport();

        return outputStream.toByteArray();
    }

}
