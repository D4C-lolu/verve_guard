package interswitch.academy.verve_guard.repositories;


import interswitch.academy.verve_guard.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, String> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);
}


