
package py.com.jaimeferreira.ccr.jhonson.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import py.com.jaimeferreira.ccr.commons.util.ManejadorDeArchivos;
import py.com.jaimeferreira.ccr.jhonson.constants.ConstantsSCJ;
import py.com.jaimeferreira.ccr.jhonson.entity.ItemSCJ;
import py.com.jaimeferreira.ccr.jhonson.entity.RespuestaCabSCJ;
import py.com.jaimeferreira.ccr.jhonson.entity.RespuestaDetSCJ;
import py.com.jaimeferreira.ccr.jhonson.repository.ItemsSCJRepository;
import py.com.jaimeferreira.ccr.jhonson.repository.RespuestaCabSCJRepository;
import py.com.jaimeferreira.ccr.jhonson.repository.RespuestaDetSCJRepository;
import py.com.jaimeferreira.ccr.jhonson.repository.RespuestaImagenSCJRepository;

/**
 *
 * @author Jaime Ferreira
 */

@Service
public class RespuestaCabSCJService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RespuestaCabSCJService.class);

    @Autowired
    RespuestaCabSCJRepository repository;

    @Autowired
    RespuestaDetSCJRepository detRepository;

    @Autowired
    RespuestaImagenSCJRepository imagenRepository;

    @Autowired
    ItemsSCJRepository itemsRepository;

    @Autowired
    private ManejadorDeArchivos manejadorDeArchivos;

    public void saveRespuestas(List<RespuestaCabSCJ> respuestas) {

        respuestas.stream().forEach((r) -> {

            // buscar si ya existe una respuesta anterior
            Optional<RespuestaCabSCJ> optional =
                repository.findByIdBocaAndUsuarioAndFechaCreacionAndActivoTrue(r.getIdBoca(), r.getUsuario(),
                                                                               r.getFechaCreacion());

            if (optional.isPresent()) {
                // marcar como falso los anteriores
                RespuestaCabSCJ exists = optional.get();
                exists.setActivo(false);
                repository.save(exists);

                // buscar los detalles
                List<RespuestaDetSCJ> existsDetails = detRepository.findByIdRespuestaCab(exists.getId());
                existsDetails.stream().forEach(d -> {
                    d.setActivo(false);

                    detRepository.save(d);

                });

            }

            RespuestaCabSCJ cab = repository.save(r);

            // detalles
            List<RespuestaDetSCJ> detalles = r.getDetalles();

            // Resolver cod_item desde items SOLO para los detalles que la app no envió ya con código.
            // Un único query (findAllById) para evitar N consultas.
            List<Long> idsSinCodigo = detalles.stream()
                .filter(d -> d.getCodItem() == null && d.getIdItem() != null)
                .map(RespuestaDetSCJ::getIdItem)
                .distinct()
                .collect(Collectors.toList());

            Map<Long, Integer> codigoPorItem = new HashMap<>();
            if (!idsSinCodigo.isEmpty()) {
                for (ItemSCJ item : itemsRepository.findAllById(idsSinCodigo)) {
                    if (item.getCodigo() != null) {
                        codigoPorItem.put(item.getId(), item.getCodigo());
                    }
                }
            }

            detalles.forEach(d -> {
                d.setIdRespuestaCab(cab.getId());

                // cod_item: respetar el que mandó la app; si no vino, resolverlo desde items.
                if (d.getCodItem() == null && d.getIdItem() != null) {
                    d.setCodItem(codigoPorItem.get(d.getIdItem()));
                }

                // "Sin Exhibición": normalizar el texto del ítem especial (id 297).
                if (d.getIdItem() != null && d.getIdItem() == ConstantsSCJ.ID_ITEM_SIN_EXHIBICION) {
                    d.setDescItem("Sin Exhibición");
                }

                detRepository.save(d);

            });
            LOGGER.info("La cantidad de imagenes es " + r.getImagenes().size());

            // imagenes
            r.getImagenes().stream().forEach(i -> {
                i.setIdRespuestaCab(cab.getId());
                if (i.getImgBase64String() != null) {
                    manejadorDeArchivos.base64ToImagen(i.getPathImagen(),
                                                       i.getImgBase64String(), r.getFechaCreacion(), false);

                    imagenRepository.save(i);
                }
            });
        });
    }

    public List<RespuestaCabSCJ> list() {

        return repository.findAll().stream().map(r -> {

            r.setDetalles(detRepository.findByIdRespuestaCab(r.getId()));
            return r;
        }).collect(Collectors.toList());

    }

}
