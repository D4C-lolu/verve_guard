package interswitch.academy.verve_guard.controllers.v1;

import interswitch.academy.verve_guard.constants.Permissions;
import interswitch.academy.verve_guard.constants.Roles;
import interswitch.academy.verve_guard.models.enums.FraudStatus;
import interswitch.academy.verve_guard.models.request.TransactionIngestionRequest;
import interswitch.academy.verve_guard.models.response.FraudAttemptResponse;
import interswitch.academy.verve_guard.services.FraudDetectionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("fraud")
@RequiredArgsConstructor
public class FraudController {

    private final FraudDetectionService fraudDetectionService;

    @PostMapping("evaluate")
    @PreAuthorize("hasAuthority('" + Permissions.TRANSACTION_CREATE + "')")
    public FraudStatus evaluate(
            @RequestBody @Valid TransactionIngestionRequest request,
            HttpServletRequest httpRequest
    ) {
        return fraudDetectionService.evaluate(request, httpRequest.getRemoteAddr());
    }

    @GetMapping("attempts")
    @PreAuthorize("hasRole('" + Roles.SUPER_ADMIN + "')")
    public Page<FraudAttemptResponse> getFraudAttempts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return fraudDetectionService.getFraudAttempts(page, size);
    }
}

