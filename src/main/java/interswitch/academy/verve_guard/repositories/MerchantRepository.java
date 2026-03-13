package interswitch.academy.verve_guard.repositories;

import interswitch.academy.verve_guard.entities.Merchant;
import interswitch.academy.verve_guard.models.enums.KycStatus;
import interswitch.academy.verve_guard.models.enums.MerchantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, String> {

    Optional<Merchant> findByUserId(String userId);

    List<Merchant> findAllByDeletedAtIsNull();

    List<Merchant> findAllByMerchantStatus(MerchantStatus merchantStatus);

    List<Merchant> findAllByKycStatus(KycStatus kycStatus);

    boolean existsByUserId(String userId);
}
