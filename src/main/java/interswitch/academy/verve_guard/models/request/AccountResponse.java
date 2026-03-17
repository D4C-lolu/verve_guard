package interswitch.academy.verve_guard.models.request;

import interswitch.academy.verve_guard.models.enums.AccountStatus;
import interswitch.academy.verve_guard.models.enums.AccountType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AccountResponse(
        String id,
        String merchantId,
        String accountNumber,
        AccountType accountType,
        String currency,
        BigDecimal balance,
        BigDecimal ledgerBalance,
        AccountStatus accountStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
