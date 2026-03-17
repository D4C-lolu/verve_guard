package interswitch.academy.verve_guard.models.response;

import interswitch.academy.verve_guard.models.enums.MerchantTier;

import java.math.BigDecimal;

public record TierConfigResponse(
        String id,
        MerchantTier tier,
        BigDecimal dailyTransactionLimit,
        BigDecimal singleTransactionLimit,
        BigDecimal monthlyTransactionLimit,
        Integer maxCards,
        Integer maxAccounts
) {}
