package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, String> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);
    
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "DELETE FROM user_roles WHERE role_id = :roleId", nativeQuery = true)
    void deleteUserAssociations(@org.springframework.data.repository.query.Param("roleId") String roleId);
}


