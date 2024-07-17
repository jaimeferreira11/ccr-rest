package py.com.jaimeferreira.ccr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import py.com.jaimeferreira.ccr.entity.Usuario;

/***
 * 
 * @author Luis Capdevila [luis_capde@hotmail.com]
 *
 */

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    public Usuario findByUsuario(String usuario);
    



    
}
