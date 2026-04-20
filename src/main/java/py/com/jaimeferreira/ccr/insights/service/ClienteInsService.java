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
import py.com.jaimeferreira.ccr.insights.dto.ClienteInsDTO;
import py.com.jaimeferreira.ccr.insights.entity.ClienteIns;
import py.com.jaimeferreira.ccr.insights.entity.Pais;
import py.com.jaimeferreira.ccr.insights.repository.ClienteInsRepository;

/**
 *
 * @author Jaime Ferreira
 */
@Service
public class ClienteInsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClienteInsService.class);

    @Autowired
    private ClienteInsRepository repository;

    @Autowired
    private PaisInsService paisService;

    public List<ClienteIns> findActivos() {
        return repository.findByEnabledTrue();
    }

    public List<ClienteIns> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "codigo"));
    }

    public List<ClienteIns> findByPais(String codPais) {
        LOGGER.info("Buscando clientes del pais: {}", codPais);
        return repository.findByPais_CodigoAndEnabledTrue(normalizeCodigo(codPais));
    }

    public ClienteIns findByCodigo(String codigo) {
        String codigoNormalizado = normalizeCodigo(codigo);
        return repository.findByCodigo(codigoNormalizado).orElseThrow(
                () -> new UnknownResourceException("Cliente con codigo " + codigoNormalizado + " no encontrado."));
    }

    public ClienteIns save(ClienteInsDTO dto, String usuario) {
        validarAlta(dto);
        String codigo = normalizeCodigo(dto.getCodigo());

        LOGGER.info("Guardando cliente: {}", codigo);

        if (repository.findByCodigo(codigo).isPresent()) {
            throw new UnknownResourceException("Cliente con codigo " + codigo + " ya existe.");
        }

        Pais pais = paisService.findByCodigo(normalizeCodigo(dto.getCodPais()));

        ClienteIns cliente = new ClienteIns();
        cliente.setCodigo(codigo);
        cliente.setDescripcion(dto.getDescripcion().trim());
        cliente.setPais(pais);
        cliente.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
        cliente.setFechaCreacion(LocalDateTime.now());
        cliente.setNombreUsuarioCreacion(usuario);

        return repository.save(cliente);
    }

    public ClienteIns update(String codigo, ClienteInsDTO dto, String usuario) {
        String codigoNormalizado = normalizeCodigo(codigo);

        LOGGER.info("Actualizando cliente: {}", codigoNormalizado);

        ClienteIns cliente = findByCodigo(codigoNormalizado);
        validarActualizacion(dto);

        if (StringUtils.hasText(dto.getDescripcion())) {
            cliente.setDescripcion(dto.getDescripcion().trim());
        }
        if (StringUtils.hasText(dto.getCodPais())) {
            Pais pais = paisService.findByCodigo(normalizeCodigo(dto.getCodPais()));
            cliente.setPais(pais);
        }
        if (dto.getEnabled() != null) {
            cliente.setEnabled(dto.getEnabled());
        }

        return repository.save(cliente);
    }

    public ClienteIns disable(String codigo, String usuario) {
        String codigoNormalizado = normalizeCodigo(codigo);
        LOGGER.info("Dando de baja cliente: {} por usuario {}", codigoNormalizado, usuario);

        ClienteIns cliente = findByCodigo(codigoNormalizado);
        cliente.setEnabled(false);
        return repository.save(cliente);
    }

    private void validarAlta(ClienteInsDTO dto) {
        if (dto == null) {
            throw new UnknownResourceException("Los datos del cliente son requeridos.");
        }
        if (!StringUtils.hasText(dto.getCodigo())) {
            throw new UnknownResourceException("El codigo del cliente es requerido.");
        }
        if (!StringUtils.hasText(dto.getDescripcion())) {
            throw new UnknownResourceException("La descripcion del cliente es requerida.");
        }
        if (!StringUtils.hasText(dto.getCodPais())) {
            throw new UnknownResourceException("El pais del cliente es requerido.");
        }
    }

    private void validarActualizacion(ClienteInsDTO dto) {
        if (dto == null) {
            throw new UnknownResourceException("Los datos del cliente son requeridos.");
        }
        if (dto.getDescripcion() != null && !StringUtils.hasText(dto.getDescripcion())) {
            throw new UnknownResourceException("La descripcion del cliente no puede estar vacia.");
        }
        if (dto.getCodPais() != null && !StringUtils.hasText(dto.getCodPais())) {
            throw new UnknownResourceException("El pais del cliente no puede estar vacio.");
        }
    }

    private String normalizeCodigo(String codigo) {
        if (!StringUtils.hasText(codigo)) {
            throw new UnknownResourceException("El codigo del cliente es requerido.");
        }
        return codigo.trim().toUpperCase();
    }

}
