package interswitch.academy.verve_guard.models.response;

import interswitch.academy.verve_guard.models.enums.FraudStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record FraudAttemptResponse(
        String id,
        String cardHash,
        String merchantId,
        String ipAddress,
        BigDecimal amount,
        String currency,
        FraudStatus status,
        List<String> flags,
        OffsetDateTime createdAt
) {}