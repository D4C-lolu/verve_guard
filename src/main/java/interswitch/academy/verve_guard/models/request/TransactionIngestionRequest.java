package interswitch.academy.verve_guard.models.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransactionIngestionRequest(
        @NotBlank String cardNumber,
        @NotBlank String merchantId,
        @NotNull BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency
) {}