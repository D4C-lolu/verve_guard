package interswitch.academy.verve_guard.models.request;

import interswitch.academy.verve_guard.models.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountRequest(
        @NotNull AccountStatus accountStatus
) {}
