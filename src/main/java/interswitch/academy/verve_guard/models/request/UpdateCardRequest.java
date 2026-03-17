package interswitch.academy.verve_guard.models.request;

import interswitch.academy.verve_guard.models.enums.CardStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateCardRequest(
        @NotNull CardStatus cardStatus
) {}
