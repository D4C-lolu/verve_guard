package interswitch.academy.verve_guard.repositories;

import interswitch.academy.verve_guard.entities.Transfer;
import interswitch.academy.verve_guard.models.enums.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, String> {

    Optional<Transfer> findByReference(String reference);

    List<Transfer> findAllByFromAccountId(String fromAccountId);

    List<Transfer> findAllByToAccountId(String toAccountId);

    List<Transfer> findAllByTransferStatus(TransferStatus transferStatus);

    boolean existsByReference(String reference);
}
