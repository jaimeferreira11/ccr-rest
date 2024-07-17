
package py.com.jaimeferreira.ccr.service;

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import py.com.jaimeferreira.ccr.entity.RespuestaCab;
import py.com.jaimeferreira.ccr.repository.RespuestaCabRepository;
import py.com.jaimeferreira.ccr.repository.RespuestaDetRepository;
import py.com.jaimeferreira.ccr.util.ManejadorDeArchivos;

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
    private ManejadorDeArchivos manejadorDeArchivos;

    public void saveRespuestas(List<RespuestaCab> respuestas) {

        respuestas.stream().forEach((r) -> {
            
            
            

            RespuestaCab cab = repository.save(r);
//            em.flush();
            
            //
            if (r.getImgBase64String() != null) {
                manejadorDeArchivos.base64ToImagen(r.getPathImagen(),
                        r.getImgBase64String(), r.getFechaCreacion());
            }

            r.getDetalles().stream().forEach(d -> {
                d.setIdRespuestaCab(cab.getId());

                detRepository.save(d);

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
