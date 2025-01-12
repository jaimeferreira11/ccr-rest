package py.com.jaimeferreira.ccr.commons.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Table(name = "usuarios", schema = "public")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @NotNull
    @Size(max = 200)
    @Column(name = "USUARIO", nullable = false, length = 200)
    private String usuario;

    @NotNull
    @Size(max = 200)
    @Column(name = "PASSWORD", nullable = false, length = 200)
    private String password;

    @NotNull
    @Size(max = 300)
    @Column(name = "NOMBRE_APELLIDO", nullable = false, length = 300)
    private String nombreApellido;

    @NotNull
    @Column(name = "ACTIVO", nullable = false, columnDefinition = "NUMBER(1) DEFAULT 1 CHECK (ACTIVO IN (0,1))")
    private Boolean activo;
    
    
    @Column(name = "COD_CLIENTE")
    private String codCliente;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNombreApellido() {
        return nombreApellido;
    }

    public void setNombreApellido(String nombreApellido) {
        this.nombreApellido = nombreApellido;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public String getCodCliente() {
        return codCliente;
    }

    public void setCodCliente(String codCliente) {
        this.codCliente = codCliente;
    }
    
    
}
