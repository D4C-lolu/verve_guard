package interswitch.academy.verve_guard.controllers.v1;


import interswitch.academy.verve_guard.constants.Permissions;
import interswitch.academy.verve_guard.constants.Roles;
import interswitch.academy.verve_guard.models.enums.AccountStatus;
import interswitch.academy.verve_guard.models.request.*;
import interswitch.academy.verve_guard.services.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasRole('"+ Roles.ADMIN +"') or hasRole('" + Roles.SUPER_ADMIN + "')")
    public AccountResponse createAccount(@RequestBody @Valid CreateAccountRequest request) {
        return accountService.createAccount(request);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("me")
    @PreAuthorize("hasRole('"+ Roles.MERCHANT +"')")
    public AccountResponse createAccountForSelf(@RequestBody @Valid CreateMyAccountRequest request) {
        return accountService.createAccountForSelf(request);
    }

    @GetMapping("{accountId}")
    @PreAuthorize("hasAuthority('" + Permissions.ACCOUNT_READ + "')")
    public AccountResponse getAccountById(@PathVariable String accountId) {
        return accountService.getAccountById(accountId);
    }

    @GetMapping("merchant/{merchantId}")
    @PreAuthorize("hasAuthority('" + Permissions.ACCOUNT_READ + "')")
    public Page<AccountResponse> getAccountsByMerchant(
            @PathVariable String merchantId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "created_at") String sortField,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection
    ) {
        return accountService.getAccountsByMerchant(merchantId, page, size, sortField, sortDirection);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("{accountId}/status")
    @PreAuthorize("hasAuthority('" + Permissions.ACCOUNT_UPDATE + "')")
    public void updateAccountStatus(
            @PathVariable String accountId,
            @RequestParam AccountStatus status
    ) {
        accountService.updateAccountStatus(accountId, status);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("{accountId}")
    @PreAuthorize("hasAuthority('" + Permissions.ACCOUNT_DELETE + "')")
    public void deleteAccount(@PathVariable String accountId) {
        accountService.deleteAccount(accountId);
    }
}