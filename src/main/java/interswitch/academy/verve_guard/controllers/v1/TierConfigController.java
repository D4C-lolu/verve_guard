package interswitch.academy.verve_guard.controllers.v1;

import interswitch.academy.verve_guard.constants.Permissions;
import interswitch.academy.verve_guard.models.enums.MerchantTier;
import interswitch.academy.verve_guard.models.request.CreateTierConfigRequest;
import interswitch.academy.verve_guard.models.request.UpdateTierConfigRequest;
import interswitch.academy.verve_guard.models.response.TierConfigResponse;
import interswitch.academy.verve_guard.services.TierConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("tier-configs")
@RequiredArgsConstructor
public class TierConfigController {

    private final TierConfigService tierConfigService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasAuthority('" + Permissions.TIER_UPDATE + "')")
    public TierConfigResponse createTierConfig(@RequestBody @Valid CreateTierConfigRequest request) {
        return tierConfigService.createTierConfig(request);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.TIER_READ + "')")
    public List<TierConfigResponse> getAllTierConfigs() {
        return tierConfigService.getAllTierConfigs();
    }

    @GetMapping("{tierConfigId}")
    @PreAuthorize("hasAuthority('" + Permissions.TIER_READ + "')")
    public TierConfigResponse getTierConfigById(@PathVariable String tierConfigId) {
        return tierConfigService.getTierConfigById(tierConfigId);
    }

    @GetMapping("tier/{tier}")
    @PreAuthorize("hasAuthority('" + Permissions.TIER_READ + "')")
    public TierConfigResponse getTierConfigByTier(@PathVariable MerchantTier tier) {
        return tierConfigService.getTierConfigByTier(tier);
    }

    @PutMapping("{tier}")
    @PreAuthorize("hasAuthority('" + Permissions.TIER_UPDATE + "')")
    public TierConfigResponse updateTierConfig(
            @PathVariable MerchantTier tier,
            @RequestBody @Valid UpdateTierConfigRequest request
    ) {
        return tierConfigService.updateTierConfig(tier, request);
    }
}