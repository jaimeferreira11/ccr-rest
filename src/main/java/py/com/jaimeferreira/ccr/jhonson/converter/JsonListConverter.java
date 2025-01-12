
package py.com.jaimeferreira.ccr.jhonson.converter;

import java.io.IOException;
import java.util.List;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author Jaime Ferreira
 */
@Converter
public class JsonListConverter implements AttributeConverter<List<String>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        try {
            return objectMapper.writeValueAsString(attribute);
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException("Error al convertir lista a JSON", e);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<String>>() {
            });
        }
        catch (IOException e) {
            throw new RuntimeException("Error al convertir JSON a lista", e);
        }
    }
}
