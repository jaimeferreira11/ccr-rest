package py.com.jaimeferreira.ccr.insights.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import py.com.jaimeferreira.ccr.commons.exception.UnknownResourceException;
import py.com.jaimeferreira.ccr.insights.dto.PaisDTO;
import py.com.jaimeferreira.ccr.insights.entity.Pais;
import py.com.jaimeferreira.ccr.insights.repository.PaisInsRepository;

/**
 *
 * @author Jaime Ferreira
 */
@Service
public class PaisInsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaisInsService.class);

    @Autowired
    private PaisInsRepository repository;

    public List<Pais> findActivos() {
        return repository.findByActivoTrue();
    }

    public List<Pais> findAll() {
        return repository.findAll();
    }

    public Pais findByCodigo(String codigo) {
        return repository.findByCodigo(codigo)
                .orElseThrow(() -> new UnknownResourceException("Pais con codigo " + codigo + " no encontrado."));
    }

    public Pais save(PaisDTO dto, String usuario) {

        LOGGER.info("Guardando pais: {}", dto.getCodigo());

        Pais pais = new Pais();
        pais.setCodigo(dto.getCodigo().trim().toUpperCase());
        pais.setDescripcion(dto.getDescripcion().trim());
        pais.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
        pais.setFechaCreacion(LocalDateTime.now());
        pais.setNombreUsuarioCreacion(usuario);

        return repository.save(pais);
    }

    public Pais update(String codigo, PaisDTO dto, String usuario) {

        LOGGER.info("Actualizando pais: {}", codigo);

        Optional<Pais> optional = repository.findByCodigo(codigo);
        if (!optional.isPresent()) {
            throw new UnknownResourceException("Pais con codigo " + codigo + " no encontrado.");
        }

        Pais pais = optional.get();
        pais.setDescripcion(dto.getDescripcion().trim());
        if (dto.getActivo() != null) {
            pais.setActivo(dto.getActivo());
        }

        return repository.save(pais);
    }

}
