package py.com.jaimeferreira.ccr.insights.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import py.com.jaimeferreira.ccr.commons.exception.UnknownResourceException;
import py.com.jaimeferreira.ccr.insights.dto.CategoriaDTO;
import py.com.jaimeferreira.ccr.insights.entity.Categoria;
import py.com.jaimeferreira.ccr.insights.entity.ClienteIns;
import py.com.jaimeferreira.ccr.insights.repository.CategoriaRepository;

/**
 *
 * @author Jaime Ferreira
 */
@Service
public class CategoriaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CategoriaService.class);

    @Autowired
    private CategoriaRepository repository;

    @Autowired
    private ClienteInsService clienteInsService;

    public List<Categoria> findAll(String codCliente) {
        if (StringUtils.hasText(codCliente)) {
            LOGGER.info("Buscando categorias para cliente: {}", codCliente);
            return repository.findByCliente_CodigoOrderByCodigoAsc(codCliente.trim().toUpperCase());
        }
        return repository.findAllByOrderByCliente_CodigoAscCodigoAsc();
    }

    public List<Categoria> findActivasByCliente(String codCliente) {
        LOGGER.info("Buscando categorias activas para cliente: {}", codCliente);
        return repository.findByCliente_CodigoAndEnabledTrueOrderByCodigoAsc(codCliente.trim().toUpperCase());
    }

    public Categoria findById(Long id) {
        return repository.findById(id).orElseThrow(
                () -> new UnknownResourceException("Categoria con id " + id + " no encontrada."));
    }

    public Categoria save(CategoriaDTO dto, String usuario) {
        validarAlta(dto);
        String codigo = dto.getCodigo().trim().toUpperCase();
        String codCliente = dto.getCodCliente().trim().toUpperCase();

        LOGGER.info("Guardando categoria: {} para cliente: {}", codigo, codCliente);

        if (repository.findByCliente_CodigoAndCodigo(codCliente, codigo).isPresent()) {
            throw new UnknownResourceException(
                    "Categoria con codigo " + codigo + " ya existe para el cliente " + codCliente + ".");
        }

        ClienteIns cliente = clienteInsService.findByCodigo(codCliente);

        Categoria categoria = new Categoria();
        categoria.setCodigo(codigo);
        categoria.setDescripcion(dto.getDescripcion().trim());
        categoria.setCliente(cliente);
        categoria.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
        categoria.setFechaCreacion(LocalDateTime.now());
        categoria.setNombreUsuarioCreacion(usuario);

        return repository.save(categoria);
    }

    public Categoria update(Long id, CategoriaDTO dto, String usuario) {
        LOGGER.info("Actualizando categoria: {}", id);

        validarActualizacion(dto);
        Categoria categoria = findById(id);

        if (StringUtils.hasText(dto.getCodigo())) {
            String nuevoCodigo = dto.getCodigo().trim().toUpperCase();
            String codCliente = categoria.getCliente().getCodigo();
            if (!nuevoCodigo.equals(categoria.getCodigo())) {
                if (repository.findByCliente_CodigoAndCodigo(codCliente, nuevoCodigo).isPresent()) {
                    throw new UnknownResourceException(
                            "Categoria con codigo " + nuevoCodigo + " ya existe para el cliente " + codCliente + ".");
                }
            }
            categoria.setCodigo(nuevoCodigo);
        }
        if (StringUtils.hasText(dto.getDescripcion())) {
            categoria.setDescripcion(dto.getDescripcion().trim());
        }
        if (StringUtils.hasText(dto.getCodCliente())) {
            ClienteIns cliente = clienteInsService.findByCodigo(dto.getCodCliente().trim().toUpperCase());
            categoria.setCliente(cliente);
        }
        if (dto.getEnabled() != null) {
            categoria.setEnabled(dto.getEnabled());
        }

        return repository.save(categoria);
    }

    public Categoria disable(Long id, String usuario) {
        LOGGER.info("Dando de baja categoria: {} por usuario {}", id, usuario);
        Categoria categoria = findById(id);
        categoria.setEnabled(false);
        return repository.save(categoria);
    }

    private void validarAlta(CategoriaDTO dto) {
        if (dto == null) {
            throw new UnknownResourceException("Los datos de la categoria son requeridos.");
        }
        if (!StringUtils.hasText(dto.getCodigo())) {
            throw new UnknownResourceException("El codigo de la categoria es requerido.");
        }
        if (!StringUtils.hasText(dto.getDescripcion())) {
            throw new UnknownResourceException("La descripcion de la categoria es requerida.");
        }
        if (!StringUtils.hasText(dto.getCodCliente())) {
            throw new UnknownResourceException("El cliente de la categoria es requerido.");
        }
    }

    private void validarActualizacion(CategoriaDTO dto) {
        if (dto == null) {
            throw new UnknownResourceException("Los datos de la categoria son requeridos.");
        }
        if (dto.getDescripcion() != null && !StringUtils.hasText(dto.getDescripcion())) {
            throw new UnknownResourceException("La descripcion de la categoria no puede estar vacia.");
        }
        if (dto.getCodCliente() != null && !StringUtils.hasText(dto.getCodCliente())) {
            throw new UnknownResourceException("El cliente de la categoria no puede estar vacio.");
        }
    }

}
