package interswitch.academy.verve_guard.models.response;

import interswitch.academy.verve_guard.models.enums.CardScheme;
import interswitch.academy.verve_guard.models.enums.CardStatus;
import interswitch.academy.verve_guard.models.enums.CardType;

import java.time.OffsetDateTime;

public record CardResponse(
        String id,
        String accountId,
        String cardNumber,
        CardType cardType,
        CardScheme scheme,
        int expiryMonth,
        int expiryYear,
        CardStatus cardStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
