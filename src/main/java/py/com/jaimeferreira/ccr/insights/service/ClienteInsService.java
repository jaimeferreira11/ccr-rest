package py.com.jaimeferreira.ccr.insights.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        return repository.findAll();
    }

    public List<ClienteIns> findByPais(String codPais) {
        LOGGER.info("Buscando clientes del pais: {}", codPais);
        return repository.findByPais_CodigoAndEnabledTrue(codPais);
    }

    public ClienteIns findByCodigo(String codigo) {
        return repository.findByCodigo(codigo)
                .orElseThrow(() -> new UnknownResourceException("Cliente con codigo " + codigo + " no encontrado."));
    }

    public ClienteIns save(ClienteInsDTO dto, String usuario) {

        LOGGER.info("Guardando cliente: {}", dto.getCodigo());

        Pais pais = paisService.findByCodigo(dto.getCodPais().trim().toUpperCase());

        ClienteIns cliente = new ClienteIns();
        cliente.setCodigo(dto.getCodigo().trim().toUpperCase());
        cliente.setDescripcion(dto.getDescripcion().trim());
        cliente.setPais(pais);
        cliente.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
        cliente.setFechaCreacion(LocalDateTime.now());
        cliente.setNombreUsuarioCreacion(usuario);

        return repository.save(cliente);
    }

    public ClienteIns update(String codigo, ClienteInsDTO dto, String usuario) {

        LOGGER.info("Actualizando cliente: {}", codigo);

        Optional<ClienteIns> optional = repository.findByCodigo(codigo);
        if (!optional.isPresent()) {
            throw new UnknownResourceException("Cliente con codigo " + codigo + " no encontrado.");
        }

        ClienteIns cliente = optional.get();
        cliente.setDescripcion(dto.getDescripcion().trim());

        if (dto.getCodPais() != null) {
            Pais pais = paisService.findByCodigo(dto.getCodPais().trim().toUpperCase());
            cliente.setPais(pais);
        }
        if (dto.getEnabled() != null) {
            cliente.setEnabled(dto.getEnabled());
        }

        return repository.save(cliente);
    }

}
