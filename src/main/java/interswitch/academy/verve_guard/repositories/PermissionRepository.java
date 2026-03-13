package interswitch.academy.verve_guard.repositories;

import interswitch.academy.verve_guard.entities.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, String> {

    Optional<Permission> findByName(String name);

    boolean existsByName(String name);
}
