
package py.com.jaimeferreira.ccr.nestle.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import py.com.jaimeferreira.ccr.commons.util.ManejadorDeArchivos;
import py.com.jaimeferreira.ccr.nestle.entity.BocaNest;
import py.com.jaimeferreira.ccr.nestle.entity.RespuestaCabNest;
import py.com.jaimeferreira.ccr.nestle.entity.RespuestaDetNest;
import py.com.jaimeferreira.ccr.nestle.repository.RespuestaCabNestRepository;
import py.com.jaimeferreira.ccr.nestle.repository.RespuestaDetNestRepository;
import py.com.jaimeferreira.ccr.nestle.repository.RespuestaImagenNestRepository;

/**
 *
 * @author Jaime Ferreira
 */

@Service
public class RespuestaCabNestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RespuestaCabNestService.class);

    @Autowired
    private EntityManager em;

    @Autowired
    RespuestaCabNestRepository repository;

    @Autowired
    RespuestaDetNestRepository detRepository;

    @Autowired
    RespuestaImagenNestRepository imagenRepository;

    @Autowired
    private ManejadorDeArchivos manejadorDeArchivos;

    @Autowired
    private BocasNestService bocasService;

    public void saveRespuestas(List<RespuestaCabNest> respuestas) {

        respuestas.stream().forEach((r) -> {

            // buscar si ya existe una respuesta anterior
            Optional<RespuestaCabNest> optional =
                repository.findByIdBocaAndUsuarioAndFechaCreacionAndActivoTrue(r.getIdBoca(), r.getUsuario(),
                                                                               r.getFechaCreacion());

            if (optional.isPresent()) {
                // marcar como falso los anteriores
                RespuestaCabNest exists = optional.get();
                exists.setActivo(false);
                repository.save(exists);

                // buscar los detalles
                List<RespuestaDetNest> existsDetails = detRepository.findByIdRespuestaCab(exists.getId());
                existsDetails.stream().forEach(d -> {
                    d.setActivo(false);

                    detRepository.save(d);

                });

            }

            RespuestaCabNest cab = repository.save(r);
            // em.flush();

            // detalles
            r.getDetalles().stream().forEach(d -> {
                d.setIdRespuestaCab(cab.getId());

                detRepository.save(d);

                // si es autoservice marcado como despensa
                
                if (cab.getCanalCcr().equalsIgnoreCase("AUTOSERVICIO")
                    && d.getCodCabecera().equalsIgnoreCase("FP") // Plaza
                    && d.getSinDatos()) {

                    Runnable task = () -> {
                        try {
                            BocaNest boca = bocasService.findById(cab.getIdBoca());
                            if (boca != null) {
                                boca.setCanalCcr("DESPENSA");
                                bocasService.save(boca);
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    };
                    Thread t = new Thread(task);
                    t.start();
                }

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

    public List<RespuestaCabNest> list() {

        return repository.findAll().stream().map(r -> {

            r.setDetalles(detRepository.findByIdRespuestaCab(r.getId()));
            return r;
        }).collect(Collectors.toList());

    }

}
