package interswitch.academy.verve_guard.models.request;

import interswitch.academy.verve_guard.models.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank String merchantId,
        @NotNull AccountType accountType,
        @NotBlank @Size(min = 3, max = 3) String currency
) {}

