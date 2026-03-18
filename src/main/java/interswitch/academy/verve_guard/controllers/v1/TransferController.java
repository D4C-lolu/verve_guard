package interswitch.academy.verve_guard.controllers.v1;

import interswitch.academy.verve_guard.constants.Permissions;
import interswitch.academy.verve_guard.constants.Roles;
import interswitch.academy.verve_guard.models.request.TransferRequest;
import interswitch.academy.verve_guard.models.response.TransferResponse;
import interswitch.academy.verve_guard.services.TransferService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public TransferResponse transfer(
            @RequestBody @Valid TransferRequest request,
            HttpServletRequest httpRequest
    ) {
        return transferService.transfer(request, httpRequest.getRemoteAddr());
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("me")
    @PreAuthorize("hasRole('"+ Roles.MERCHANT +"')")
    public TransferResponse transferForSelf(
            @RequestBody @Valid TransferRequest request,
            HttpServletRequest httpRequest
    ) {
        return transferService.transferForSelf(request, httpRequest.getRemoteAddr());
    }

    @GetMapping("{transferId}")
    @PreAuthorize("hasAuthority('" + Permissions.TRANSFER_READ + "')")
    public TransferResponse getTransferById(@PathVariable String transferId) {
        return transferService.getTransferById(transferId);
    }

    @GetMapping("account/{accountId}")
    @PreAuthorize("hasAuthority('" + Permissions.TRANSFER_READ + "')")
    public Page<TransferResponse> getTransfersByAccount(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return transferService.getTransfersByAccount(accountId, page, size);
    }
}