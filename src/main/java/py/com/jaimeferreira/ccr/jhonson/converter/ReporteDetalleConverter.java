
package py.com.jaimeferreira.ccr.jhonson.converter;

import java.io.IOException;
import java.util.List;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import py.com.jaimeferreira.ccr.jhonson.dto.ReporteDetalleDTO;

/**
 *
 * @author Jaime Ferreira
 */
@Converter
public class ReporteDetalleConverter implements AttributeConverter<List<ReporteDetalleDTO>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<ReporteDetalleDTO> detalles) {
        try {
            return objectMapper.writeValueAsString(detalles);
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException("Error al convertir lista de detalles a JSON", e);
        }
    }

    @Override
    public List<ReporteDetalleDTO> convertToEntityAttribute(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<ReporteDetalleDTO>>() {
            });
        }
        catch (IOException e) {
            throw new RuntimeException("Error al convertir JSON a lista de detalles", e);
        }
    }
}
