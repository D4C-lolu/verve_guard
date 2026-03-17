package interswitch.academy.verve_guard.controllers.v1;

import interswitch.academy.verve_guard.annotation.ValidSortField;
import interswitch.academy.verve_guard.constants.Permissions;
import interswitch.academy.verve_guard.models.enums.KycStatus;
import interswitch.academy.verve_guard.models.enums.MerchantStatus;
import interswitch.academy.verve_guard.models.request.CreateMerchantRequest;
import interswitch.academy.verve_guard.models.request.MerchantSignupRequest;
import interswitch.academy.verve_guard.models.request.UpdateMerchantRequest;
import interswitch.academy.verve_guard.models.response.MerchantResponse;
import interswitch.academy.verve_guard.services.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasAuthority('" + Permissions.MERCHANT_CREATE + "')")
    public MerchantResponse createMerchant(@RequestBody @Valid CreateMerchantRequest request) {
        return merchantService.createMerchant(request);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("register")
    public MerchantResponse registerNewUserAsMerchant(@RequestBody @Valid MerchantSignupRequest request) {
        return merchantService.registerNewUserAsMerchant(request);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("self-register")
    public MerchantResponse selfRegisterExistingUser(@RequestParam(required = false) String address) {
        return merchantService.selfRegisterExistingUser(address);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.MERCHANT_READ + "')")
    public Page<MerchantResponse> getAllMerchants(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @ValidSortField(target = MerchantResponse.class) @RequestParam(defaultValue = "createdAt") String sortField,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection
    ) {
        return merchantService.getAllMerchants(page, size, sortField, sortDirection);
    }

    @GetMapping("status")
    @PreAuthorize("hasAuthority('" + Permissions.MERCHANT_READ + "')")
    public Page<MerchantResponse> getMerchantsByStatus(
            @RequestParam MerchantStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @ValidSortField(target = MerchantResponse.class) @RequestParam(defaultValue = "createdAt") String sortField,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection
    ) {
        return merchantService.getMerchantsByStatus(status, page, size, sortField, sortDirection);
    }

    @GetMapping("kyc-status")
    @PreAuthorize("hasAuthority('" + Permissions.MERCHANT_READ + "')")
    public Page<MerchantResponse> getMerchantsByKycStatus(
            @RequestParam KycStatus kycStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @ValidSortField(target = MerchantResponse.class) @RequestParam(defaultValue = "createdAt") String sortField,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection
    ) {
        return merchantService.getMerchantsByKycStatus(kycStatus, page, size, sortField, sortDirection);
    }

    @GetMapping("{merchantId}")
    @PreAuthorize("hasAuthority('" + Permissions.MERCHANT_READ + "')")
    public MerchantResponse getMerchantById(@PathVariable String merchantId) {
        return merchantService.getMerchantById(merchantId);
    }

    @PutMapping("{merchantId}")
    @PreAuthorize("hasAuthority('" + Permissions.MERCHANT_UPDATE + "')")
    public MerchantResponse updateMerchant(
            @PathVariable String merchantId,
            @RequestBody @Valid UpdateMerchantRequest request
    ) {
        return merchantService.updateMerchant(merchantId, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("{merchantId}/status")
    @PreAuthorize("hasAuthority('" + Permissions.MERCHANT_UPDATE + "')")
    public void updateMerchantStatus(
            @PathVariable String merchantId,
            @RequestParam MerchantStatus status
    ) {
        merchantService.updateMerchantStatus(merchantId, status);
    }

    @PatchMapping("{merchantId}/kyc")
    @PreAuthorize("hasAuthority('" + Permissions.MERCHANT_KYC + "')")
    public MerchantResponse updateKycStatus(
            @PathVariable String merchantId,
            @RequestParam KycStatus kycStatus
    ) {
        return merchantService.updateKycStatus(merchantId, kycStatus);
    }

    @PatchMapping("{merchantId}/tier/upgrade")
    @PreAuthorize("hasAuthority('" + Permissions.MERCHANT_UPDATE + "')")
    public MerchantResponse upgradeTier(@PathVariable String merchantId) {
        return merchantService.upgradeTier(merchantId);
    }

    @PatchMapping("{merchantId}/tier/downgrade")
    @PreAuthorize("hasAuthority('" + Permissions.MERCHANT_UPDATE + "')")
    public MerchantResponse downgradeTier(@PathVariable String merchantId) {
        return merchantService.downgradeTier(merchantId);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("{merchantId}")
    @PreAuthorize("hasAuthority('" + Permissions.MERCHANT_DELETE + "')")
    public void deleteMerchant(@PathVariable String merchantId) {
        merchantService.deleteMerchant(merchantId);
    }
}