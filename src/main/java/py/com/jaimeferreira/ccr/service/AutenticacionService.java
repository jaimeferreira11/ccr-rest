package py.com.jaimeferreira.ccr.service;

import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

// import py.com.jaimeferreira.ccr.entity.SessionDeUsuario;
import py.com.jaimeferreira.ccr.entity.Usuario;
import py.com.jaimeferreira.ccr.repository.UsuarioRepository;

@Service
public class AutenticacionService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    EntityManager em;

    // @Autowired
    // private SessionDeUsuarioRepository sessionDeUsuarioRepository;

    public Usuario findByUsernameAndPassword(String username, String password) {
        Usuario usuario = usuarioRepository.findByUsuarioIgnoreCase(username);
        // No se va usar encriptacion
        // if (this.verifyPassword(password, usuario.getPassword())) {
        // return usuario;
        // }
        if(usuario == null) return null;
        if (password.equals(usuario.getPassword())) {
            return usuario;
        }
        return null;
    }

    public Usuario findByUsuario(String username) {
        Usuario usuario = usuarioRepository.findByUsuarioIgnoreCase(username);
        return usuario;
    }

    public String encodePassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(8));
    }

    public boolean verifyPassword(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }

    // public Usuario setPasswordToUser(Integer idUsuario, String usuarioActualizacion, String
    // password) {
    // Usuario usuario = usuarioRepository.findById(idUsuario).get();
    // usuario.setPassword(this.encodePassword(password));
    // usuario.setNombreUsuarioActualizacion(usuarioActualizacion);
    // usuario.setFechaActualizacion(LocalDateTime.now());
    // usuario = usuarioRepository.save(usuario);
    // return usuario;
    // }

    // public SessionDeUsuario setSessionDeUsuario(Usuario usuario, String token, Date
    // tiempoExpiracion) {
    // SessionDeUsuario sessionDeUsuario = new SessionDeUsuario();
    // sessionDeUsuario.setUsuario(usuario);
    // sessionDeUsuario.setToken(token);
    // LocalDateTime localDateTimeTiempoExpiracion =
    // tiempoExpiracion.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    // sessionDeUsuario.setTiempoExpiracion(localDateTimeTiempoExpiracion);
    // sessionDeUsuario.setFechaCreacion(LocalDateTime.now());
    // sessionDeUsuario.setNombreUsuarioCreacion("SYSTEM");
    // return this.sessionDeUsuarioRepository.save(sessionDeUsuario);
    // }

    public void updateUser(Usuario user) {

        usuarioRepository.save(user);

    }

}
