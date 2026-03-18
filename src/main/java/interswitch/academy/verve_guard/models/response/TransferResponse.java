package interswitch.academy.verve_guard.models.response;

import interswitch.academy.verve_guard.models.enums.TransferStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransferResponse(
        String id,
        String reference,
        String fromAccountId,
        String toAccountId,
        BigDecimal amount,
        String currency,
        TransferStatus transferStatus,
        String description,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}