
package py.com.jaimeferreira.ccr.bebidaspy.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import py.com.jaimeferreira.ccr.bebidaspy.entity.RespuestaCab;
import py.com.jaimeferreira.ccr.bebidaspy.entity.RespuestaDet;
import py.com.jaimeferreira.ccr.bebidaspy.repository.RespuestaCabRepository;
import py.com.jaimeferreira.ccr.bebidaspy.repository.RespuestaDetRepository;
import py.com.jaimeferreira.ccr.bebidaspy.repository.RespuestaImagenRepository;
import py.com.jaimeferreira.ccr.commons.util.ManejadorDeArchivos;

/**
 *
 * @author Jaime Ferreira
 */

@Service
public class RespuestaCabService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RespuestaCabService.class);

    @Autowired
    private EntityManager em;

    @Autowired
    RespuestaCabRepository repository;

    @Autowired
    RespuestaDetRepository detRepository;

    @Autowired
    RespuestaImagenRepository imagenRepository;

    @Autowired
    private ManejadorDeArchivos manejadorDeArchivos;

    public void saveRespuestas(List<RespuestaCab> respuestas) {

        respuestas.stream().forEach((r) -> {

            // buscar si ya existe una respuesta anterior
            Optional<RespuestaCab> optional =
                repository.findByIdBocaAndUsuarioAndFechaCreacionAndActivoTrue(r.getIdBoca(), r.getUsuario(),
                                                                               r.getFechaCreacion());

            if (optional.isPresent()) {
                // marcar como falso los anteriores
                RespuestaCab exists = optional.get();
                exists.setActivo(false);
                repository.save(exists);

                // buscar los detalles
                List<RespuestaDet> existsDetails = detRepository.findByIdRespuestaCab(exists.getId());
                existsDetails.stream().forEach(d -> {
                    d.setActivo(false);

                    detRepository.save(d);

                });

            }

            RespuestaCab cab = repository.save(r);
            // em.flush();

            // imagen de portada --> quitar
//            if (r.getImgBase64String() != null) {
//                manejadorDeArchivos.base64ToImagen(r.getPathImagen(),
//                                                   r.getImgBase64String(), r.getFechaCreacion());
//            }

            // detalles
            r.getDetalles().stream().forEach(d -> {
                d.setIdRespuestaCab(cab.getId());

                detRepository.save(d);

            });

            // imagenes
            r.getImagenes().stream().forEach(i -> {
                i.setIdRespuestaCab(cab.getId());
                if (i.getImgBase64String() != null) {
                    manejadorDeArchivos.base64ToImagen(i.getPathImagen(),
                                                       i.getImgBase64String(), r.getFechaCreacion(), true);
                    
                    imagenRepository.save(i);
                }
            });
        });
    }

    public List<RespuestaCab> list() {

        return repository.findAll().stream().map(r -> {

            r.setDetalles(detRepository.findByIdRespuestaCab(r.getId()));
            return r;
        }).collect(Collectors.toList());

    }

}
