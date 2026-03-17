package interswitch.academy.verve_guard.models.request;

import interswitch.academy.verve_guard.models.enums.CardScheme;
import interswitch.academy.verve_guard.models.enums.CardType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMyCardRequest(
        @NotBlank String accountId,
        @NotBlank String cardNumber,
        @NotNull CardType cardType,
        @NotNull CardScheme scheme,
        @Min(1) @Max(12) int expiryMonth,
        @Min(2025) int expiryYear
) {}