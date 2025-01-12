package py.com.jaimeferreira.ccr.commons.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import py.com.jaimeferreira.ccr.commons.entity.Usuario;

/***
 * 
 * @author Luis Capdevila [luis_capde@hotmail.com]
 *
 */

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    public Usuario findByUsuarioIgnoreCase(String usuario);
    
    
    public Optional<Usuario> findByUsuarioIgnoreCaseAndCodClienteIgnoreCaseAndActivoTrue(String usuario, String codCliente);
    



    
}
