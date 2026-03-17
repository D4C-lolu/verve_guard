package interswitch.academy.verve_guard.models.projections;

import interswitch.academy.verve_guard.models.enums.KycStatus;
import interswitch.academy.verve_guard.models.enums.MerchantStatus;
import interswitch.academy.verve_guard.models.enums.MerchantTier;
import interswitch.academy.verve_guard.models.enums.UserStatus;

import java.time.OffsetDateTime;

public interface MerchantProjection {

    String getId();

    String getAddress();

    KycStatus getKycStatus();

    MerchantStatus getMerchantStatus();

    MerchantTier getTier();

    String getUserId();

    String getUserFirstname();

    String getUserLastname();

    String getUserEmail();

    String getUserPhone();

    UserStatus getUserStatus();

    OffsetDateTime getCreatedAt();

    OffsetDateTime getUpdatedAt();
}