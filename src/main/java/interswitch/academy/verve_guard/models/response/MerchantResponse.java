package interswitch.academy.verve_guard.models.response;

import interswitch.academy.verve_guard.models.enums.KycStatus;
import interswitch.academy.verve_guard.models.enums.MerchantStatus;
import interswitch.academy.verve_guard.models.enums.MerchantTier;
import interswitch.academy.verve_guard.models.enums.UserStatus;

import java.time.OffsetDateTime;

public record MerchantResponse(
        String id,
        String address,
        KycStatus kycStatus,
        MerchantStatus merchantStatus,
        MerchantTier tier,
        String userId,
        String userFirstname,
        String userLastname,
        String userEmail,
        String userPhone,
        UserStatus userStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}