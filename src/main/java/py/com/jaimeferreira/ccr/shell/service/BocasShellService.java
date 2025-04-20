
package py.com.jaimeferreira.ccr.shell.service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import py.com.jaimeferreira.ccr.shell.entity.BocaShell;
import py.com.jaimeferreira.ccr.shell.repository.BocasShellRepository;

/**
 *
 * @author Jaime Ferreira
 */

@Service
public class BocasShellService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BocasShellService.class);

    private final double distanciaEnKm = 0.1;

    @Autowired
    BocasShellRepository repository;

    public BocaShell findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public List<BocaShell> list() {
        return repository.findByActivoTrue();
    }

    public List<BocaShell> findByProximity(String latitude, String longitude, Integer cantidad) {

        LOGGER.info("Buscando bocas por proximidad: " + latitude + "," + longitude);

        List<BocaShell> list = repository.findByActivoTrueAndLatitudIsNotNullAndLongitudIsNotNull();

        if (cantidad != null) {
            LOGGER.info("Buscando sugerencias.. ");
            return list.stream().peek(s -> {
                if (!s.getLatitud().isEmpty() && !s.getLongitud().isEmpty())
                    s.setDistancia(distanciaCoord(latitude, longitude, s.getLongitud(), s.getLatitud()));

            })
                       .filter(s -> !s.getLatitud().isEmpty() && !s.getLongitud().isEmpty())
                       .sorted(Comparator.comparingDouble(BocaShell::getDistancia))
                       .limit(cantidad)
                       .collect(Collectors.toList());
        }

        return list.stream().peek(s -> {
            if (!s.getLatitud().isEmpty() && !s.getLongitud().isEmpty())
                s.setDistancia(distanciaCoord(latitude, longitude, s.getLongitud(), s.getLatitud()));

        }).filter(

                  s -> !s.getLatitud().isEmpty() && !s.getLongitud().isEmpty() && s.getDistancia() < distanciaEnKm)
                   .sorted(Comparator.comparingDouble(BocaShell::getDistancia))
                   .limit(1)
                   .collect(Collectors.toList());
    }

    public void save(BocaShell boca) {
        repository.save(boca);
    }

    private double distanciaCoord(String latitud1, String longitud1, String latitud2, String longitud2) {
        // double radioTierra = 3958.75;//en millas
        double radioTierra = 6371;// en kilómetros

        double lat1 = Double.parseDouble(latitud1.trim().replaceAll(",", ""));
        double lng1 = Double.parseDouble(longitud1.trim().replaceAll(",", ""));

        double lat2 = Double.parseDouble(latitud2.trim().replaceAll(",", ""));
        double lng2 = Double.parseDouble(longitud2.trim().replaceAll(",", ""));

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double sindLat = Math.sin(dLat / 2);
        double sindLng = Math.sin(dLng / 2);
        double va1 = Math.pow(sindLat, 2)
                     + Math.pow(sindLng, 2) * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));
        double va2 = 2 * Math.atan2(Math.sqrt(va1), Math.sqrt(1 - va1));
        double distancia = radioTierra * va2;

        LOGGER.info("La distancia es " + distancia);

        return distancia; // en kilometro
    }

}
