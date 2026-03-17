package interswitch.academy.verve_guard.models.request;

import jakarta.validation.constraints.NotBlank;

public record CreateMerchantRequest(
        @NotBlank String userId,
        String address
) {}

