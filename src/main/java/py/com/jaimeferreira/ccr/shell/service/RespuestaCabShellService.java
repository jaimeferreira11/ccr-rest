
package py.com.jaimeferreira.ccr.shell.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import py.com.jaimeferreira.ccr.commons.util.ManejadorDeArchivos;
import py.com.jaimeferreira.ccr.shell.entity.RespuestaCabShell;
import py.com.jaimeferreira.ccr.shell.entity.RespuestaDetShell;
import py.com.jaimeferreira.ccr.shell.repository.RespuestaCabShellRepository;
import py.com.jaimeferreira.ccr.shell.repository.RespuestaDetShellRepository;
import py.com.jaimeferreira.ccr.shell.repository.RespuestaMultimediaShellRepository;

/**
 *
 * @author Jaime Ferreira
 */

@Service
public class RespuestaCabShellService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RespuestaCabShellService.class);

    @Autowired
    private EntityManager em;

    @Autowired
    RespuestaCabShellRepository repository;

    @Autowired
    RespuestaDetShellRepository detRepository;

    @Autowired
    RespuestaMultimediaShellRepository multimediaRepository;

    @Autowired
    private ManejadorDeArchivos manejadorDeArchivos;

    @Autowired
    private BocasShellService bocasService;

    public Long saveRespuesta(RespuestaCabShell r) {

        // buscar si ya existe una respuesta anterior
        Optional<RespuestaCabShell> optional =
            repository.findByIdBocaAndUsuarioAndFechaCreacionAndActivoTrue(r.getIdBoca(), r.getUsuario(),
                                                                           r.getFechaCreacion());

        if (optional.isPresent()) {
            // ! Si existe, retornar el id
            return optional.get().getId();
            
            
            // marcar como falso los anteriores
//            RespuestaCabShell exists = optional.get();
//            exists.setActivo(false);
//            repository.save(exists);

            // buscar los detalles
//            List<RespuestaDetShell> existsDetails = detRepository.findByIdRespuestaCab(exists.getId());
//            existsDetails.stream().forEach(d -> {
//                d.setActivo(false);
//                detRepository.save(d);
//
//            });

        }

        RespuestaCabShell cab = repository.save(r);
        // em.flush();

        // detalles
        r.getDetalles().stream().forEach(d -> {
            d.setIdRespuestaCab(cab.getId());
            detRepository.save(d);

        });
        
        return cab.getId();
        // LOGGER.info("La cantidad de multimedia es " + r.getMultimedia().size());
        //
        // // imagenes
        // r.getMultimedia().stream().forEach(i -> {
        // i.setIdRespuestaCab(cab.getId());
        // if (i.getPathBase64String() != null) {
        // manejadorDeArchivos.base64ToImagen(i.getPath(),
        // i.getPathBase64String(), r.getFechaCreacion(), false);
        //
        // multimediaRepository.save(i);
        // }
        // });

    }

    public void saveRespuestas(List<RespuestaCabShell> respuestas) {

        respuestas.stream().forEach((r) -> {

            // buscar si ya existe una respuesta anterior
            Optional<RespuestaCabShell> optional =
                repository.findByIdBocaAndUsuarioAndFechaCreacionAndActivoTrue(r.getIdBoca(), r.getUsuario(),
                                                                               r.getFechaCreacion());

            if (optional.isPresent()) {
                // marcar como falso los anteriores
                RespuestaCabShell exists = optional.get();
                exists.setActivo(false);
                repository.save(exists);

                // buscar los detalles
                List<RespuestaDetShell> existsDetails = detRepository.findByIdRespuestaCab(exists.getId());
                existsDetails.stream().forEach(d -> {
                    d.setActivo(false);

                    detRepository.save(d);

                });

            }

            RespuestaCabShell cab = repository.save(r);
            // em.flush();

            // detalles
            r.getDetalles().stream().forEach(d -> {
                d.setIdRespuestaCab(cab.getId());

                detRepository.save(d);

            });
            LOGGER.info("La cantidad de multimedia es " + r.getMultimedia().size());

            // imagenes
            r.getMultimedia().stream().forEach(i -> {
                i.setIdRespuestaCab(cab.getId());
                if (i.getPathBase64String() != null) {
                    manejadorDeArchivos.base64ToImagen(i.getPath(),
                                                       i.getPathBase64String(), r.getFechaCreacion(), false);

                    multimediaRepository.save(i);
                }
            });
        });
    }

    public List<RespuestaCabShell> list() {

        return repository.findAll().stream().map(r -> {

            r.setDetalles(detRepository.findByIdRespuestaCab(r.getId()));
            return r;
        }).collect(Collectors.toList());

    }

}
