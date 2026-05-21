package py.com.jaimeferreira.ccr.lt.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import py.com.jaimeferreira.ccr.lt.dto.*;
import py.com.jaimeferreira.ccr.lt.entity.*;
import py.com.jaimeferreira.ccr.lt.repository.*;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LtIntegracionService {

    private static final String USUARIO_SISTEMA = "LT-API";

    @Autowired
    private LtSucursalRepository sucursalRepo;

    @Autowired
    private LtProductoRepository productoRepo;

    @Autowired
    private LtTicketRepository ticketRepo;

    @Autowired
    private LtPersonaRepository personaRepo;

    @Transactional
    public int guardarSucursales(List<SucursalDTO> lista) {
        for (SucursalDTO dto : lista) {
            LtSucursal entity = sucursalRepo.findByPunto(dto.getPunto())
                    .orElseGet(() -> {
                        LtSucursal nueva = new LtSucursal();
                        nueva.setFechaCreacion(LocalDateTime.now());
                        nueva.setNombreUsuarioCreacion(USUARIO_SISTEMA);
                        return nueva;
                    });
            entity.setPunto(dto.getPunto());
            entity.setDireccion(dto.getDireccion());
            entity.setProvincia(dto.getProvincia());
            entity.setCiudad(dto.getCiudad());
            entity.setMts2(dto.getMts2());
            entity.setFechaActualizacion(LocalDateTime.now());
            entity.setNombreUsuarioActualizacion(USUARIO_SISTEMA);
            sucursalRepo.save(entity);
        }
        return lista.size();
    }

    @Transactional
    public int guardarProductos(List<ProductoDTO> lista) {
        for (ProductoDTO dto : lista) {
            LtProducto entity = productoRepo.findByEancode(dto.getEancode())
                    .orElseGet(() -> {
                        LtProducto nuevo = new LtProducto();
                        nuevo.setFechaCreacion(LocalDateTime.now());
                        nuevo.setNombreUsuarioCreacion(USUARIO_SISTEMA);
                        return nuevo;
                    });
            entity.setEancode(dto.getEancode());
            entity.setDescripcion(dto.getDescripcion());
            entity.setIdSector(dto.getIdSector());
            entity.setSector(dto.getSector());
            entity.setIdSeccion(dto.getIdSeccion());
            entity.setSeccion(dto.getSeccion());
            entity.setIdCategoria(dto.getIdCategoria());
            entity.setCategoria(dto.getCategoria());
            entity.setIdSubcategoria(dto.getIdSubcategoria());
            entity.setSubcategoria(dto.getSubcategoria());
            entity.setFabricante(dto.getFabricante());
            entity.setMarca(dto.getMarca());
            entity.setContenido(dto.getContenido());
            entity.setPesovolumen(dto.getPesovolumen());
            entity.setUnidadMedida(dto.getUnidadMedida());
            entity.setFechaActualizacion(LocalDateTime.now());
            entity.setNombreUsuarioActualizacion(USUARIO_SISTEMA);
            productoRepo.save(entity);
        }
        return lista.size();
    }

    @Transactional
    public int guardarTickets(List<TicketDTO> lista) {
        for (TicketDTO dto : lista) {
            LtTicket entity = ticketRepo
                    .findByPuntoAndNroTicketAndEancode(dto.getPunto(), dto.getNroTicket(), dto.getEancode())
                    .orElseGet(() -> {
                        LtTicket nuevo = new LtTicket();
                        nuevo.setFechaCreacion(LocalDateTime.now());
                        nuevo.setNombreUsuarioCreacion(USUARIO_SISTEMA);
                        return nuevo;
                    });
            entity.setPunto(dto.getPunto());
            entity.setNroTicket(dto.getNroTicket());
            entity.setFecha(dto.getFecha());
            entity.setHora(dto.getHora());
            entity.setEancode(dto.getEancode());
            entity.setEanDesc(dto.getEan_desc());
            entity.setUnidadesVendidas(dto.getUnidades_vendidas());
            entity.setPrecioRegular(dto.getPrecio_regular());
            entity.setPrecioPromocional(dto.getPrecio_promocional());
            entity.setTipoVenta(dto.getTipo_venta());
            entity.setFechaActualizacion(LocalDateTime.now());
            entity.setNombreUsuarioActualizacion(USUARIO_SISTEMA);
            ticketRepo.save(entity);
        }
        return lista.size();
    }

    @Transactional
    public int guardarPersonas(List<PersonaDTO> lista) {
        for (PersonaDTO dto : lista) {
            LtPersona entity = personaRepo
                    .findByPuntoAndNroTicketAndIdentificacion(dto.getPunto(), dto.getNroTicket(), dto.getIdentificacion())
                    .orElseGet(() -> {
                        LtPersona nueva = new LtPersona();
                        nueva.setFechaCreacion(LocalDateTime.now());
                        nueva.setNombreUsuarioCreacion(USUARIO_SISTEMA);
                        return nueva;
                    });
            entity.setPunto(dto.getPunto());
            entity.setNroTicket(dto.getNroTicket());
            entity.setFecha(dto.getFecha());
            entity.setIdentificacion(dto.getIdentificacion());
            entity.setNombreYApellidoEmpresa(dto.getNombreyapellidoempresa());
            entity.setFechaActualizacion(LocalDateTime.now());
            entity.setNombreUsuarioActualizacion(USUARIO_SISTEMA);
            personaRepo.save(entity);
        }
        return lista.size();
    }
}
