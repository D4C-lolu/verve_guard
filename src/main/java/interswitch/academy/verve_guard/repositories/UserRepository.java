package interswitch.academy.verve_guard.repositories;

import interswitch.academy.verve_guard.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);


    boolean existsByPhone(String phone);

    List<User> findAllByDeletedAtIsNull();

    Page<User> findAllByDeletedAtIsNull(Pageable pageable);
}
