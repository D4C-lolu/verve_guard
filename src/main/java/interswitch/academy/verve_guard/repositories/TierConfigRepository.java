package interswitch.academy.verve_guard.repositories;

import interswitch.academy.verve_guard.entities.TierConfig;
import interswitch.academy.verve_guard.models.enums.MerchantTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TierConfigRepository extends JpaRepository<TierConfig, String> {

    Optional<TierConfig> findByTier(MerchantTier tier);

    boolean existsByTier(MerchantTier tier);
}
