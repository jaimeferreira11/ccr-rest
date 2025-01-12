package py.com.jaimeferreira.ccr.commons.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import py.com.jaimeferreira.ccr.commons.entity.UserRole;

/***
 * 
 * @author Jaime Ferreira
 *
 */

@Repository
public interface UserRolesRepository extends JpaRepository<UserRole, Long> {

    public List<UserRole> findByUsuarioIgnoreCase(String usuario);

}
