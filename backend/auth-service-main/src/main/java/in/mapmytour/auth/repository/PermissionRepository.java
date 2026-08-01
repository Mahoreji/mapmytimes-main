package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, String> {

    Optional<Permission> findByCode(String code);

    boolean existsByCode(String code);
}


