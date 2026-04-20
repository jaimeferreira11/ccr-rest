package py.com.jaimeferreira.ccr.insights.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
        return repository.findAll(Sort.by(Sort.Direction.ASC, "codigo"));
    }

    public Pais findByCodigo(String codigo) {
        String codigoNormalizado = normalizeCodigo(codigo);
        return repository.findByCodigo(codigoNormalizado)
                .orElseThrow(() -> new UnknownResourceException("Pais con codigo " + codigoNormalizado + " no encontrado."));
    }

    public Pais save(PaisDTO dto, String usuario) {
        validarAlta(dto);
        String codigo = normalizeCodigo(dto.getCodigo());

        LOGGER.info("Guardando pais: {}", codigo);

        if (repository.findByCodigo(codigo).isPresent()) {
            throw new UnknownResourceException("Pais con codigo " + codigo + " ya existe.");
        }

        Pais pais = new Pais();
        pais.setCodigo(codigo);
        pais.setDescripcion(dto.getDescripcion().trim());
        pais.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
        pais.setFechaCreacion(LocalDateTime.now());
        pais.setNombreUsuarioCreacion(usuario);

        return repository.save(pais);
    }

    public Pais update(String codigo, PaisDTO dto, String usuario) {
        String codigoNormalizado = normalizeCodigo(codigo);

        LOGGER.info("Actualizando pais: {}", codigoNormalizado);

        Pais pais = findByCodigo(codigoNormalizado);
        validarActualizacion(dto);
        if (StringUtils.hasText(dto.getDescripcion())) {
            pais.setDescripcion(dto.getDescripcion().trim());
        }
        if (dto.getActivo() != null) {
            pais.setActivo(dto.getActivo());
        }

        return repository.save(pais);
    }

    public Pais disable(String codigo, String usuario) {
        String codigoNormalizado = normalizeCodigo(codigo);
        LOGGER.info("Dando de baja pais: {} por usuario {}", codigoNormalizado, usuario);

        Pais pais = findByCodigo(codigoNormalizado);
        pais.setActivo(false);
        return repository.save(pais);
    }

    private void validarAlta(PaisDTO dto) {
        if (dto == null) {
            throw new UnknownResourceException("Los datos del pais son requeridos.");
        }
        if (!StringUtils.hasText(dto.getCodigo())) {
            throw new UnknownResourceException("El codigo del pais es requerido.");
        }
        if (!StringUtils.hasText(dto.getDescripcion())) {
            throw new UnknownResourceException("La descripcion del pais es requerida.");
        }
    }

    private void validarActualizacion(PaisDTO dto) {
        if (dto == null) {
            throw new UnknownResourceException("Los datos del pais son requeridos.");
        }
        if (dto.getDescripcion() != null && !StringUtils.hasText(dto.getDescripcion())) {
            throw new UnknownResourceException("La descripcion del pais no puede estar vacia.");
        }
    }

    private String normalizeCodigo(String codigo) {
        if (!StringUtils.hasText(codigo)) {
            throw new UnknownResourceException("El codigo del pais es requerido.");
        }
        return codigo.trim().toUpperCase();
    }

}
