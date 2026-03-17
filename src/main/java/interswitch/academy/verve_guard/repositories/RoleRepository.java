package interswitch.academy.verve_guard.repositories;


import interswitch.academy.verve_guard.entities.Role;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.Optional;

import static org.hibernate.jpa.HibernateHints.HINT_CACHEABLE;

public interface RoleRepository extends JpaRepository<Role, String> {

    @QueryHints(@QueryHint(name = HINT_CACHEABLE, value = "true"))
    Optional<Role> findByName(String name);

    boolean existsByName(String name);
}


