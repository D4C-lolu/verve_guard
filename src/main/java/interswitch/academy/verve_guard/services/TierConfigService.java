package interswitch.academy.verve_guard.services;

import com.github.f4b6a3.ulid.UlidCreator;
import interswitch.academy.verve_guard.entities.TierConfig;
import interswitch.academy.verve_guard.exceptions.ConflictException;
import interswitch.academy.verve_guard.exceptions.NotFoundException;
import interswitch.academy.verve_guard.mapper.TierConfigMapper;
import interswitch.academy.verve_guard.models.enums.MerchantTier;
import interswitch.academy.verve_guard.models.request.CreateTierConfigRequest;
import interswitch.academy.verve_guard.models.request.UpdateTierConfigRequest;
import interswitch.academy.verve_guard.models.response.TierConfigResponse;
import interswitch.academy.verve_guard.repositories.TierConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TierConfigService {

    private final TierConfigRepository tierConfigRepository;
    private final TierConfigMapper tierConfigMapper;

    @Transactional
    public TierConfigResponse createTierConfig(CreateTierConfigRequest request) {
        if (tierConfigRepository.existsByTier(request.tier())) {
            throw new ConflictException("Tier config already exists for " + request.tier());
        }

        TierConfig tierConfig = TierConfig.builder()
                .id(UlidCreator.getUlid().toString())
                .tier(request.tier())
                .dailyTransactionLimit(request.dailyTransactionLimit())
                .singleTransactionLimit(request.singleTransactionLimit())
                .monthlyTransactionLimit(request.monthlyTransactionLimit())
                .maxCards(request.maxCards())
                .maxAccounts(request.maxAccounts())
                .build();

        return tierConfigMapper.map(tierConfigRepository.save(tierConfig));
    }

    @Transactional
    public TierConfigResponse updateTierConfig(MerchantTier tier, UpdateTierConfigRequest request) {
        TierConfig tierConfig = tierConfigRepository.findByTier(tier)
                .orElseThrow(() -> new NotFoundException("Tier config not found"));

        tierConfig.setDailyTransactionLimit(request.dailyTransactionLimit());
        tierConfig.setSingleTransactionLimit(request.singleTransactionLimit());
        tierConfig.setMonthlyTransactionLimit(request.monthlyTransactionLimit());
        tierConfig.setMaxCards(request.maxCards());
        tierConfig.setMaxAccounts(request.maxAccounts());

        return tierConfigMapper.map(tierConfigRepository.save(tierConfig));
    }

    public TierConfigResponse getTierConfigById(String tierConfigId) {
        return tierConfigMapper.map(tierConfigRepository.findById(tierConfigId)
                .orElseThrow(() -> new NotFoundException("Tier config not found")));
    }

    public TierConfigResponse getTierConfigByTier(MerchantTier tier) {
        return tierConfigMapper.map(tierConfigRepository.findByTier(tier)
                .orElseThrow(() -> new NotFoundException("Tier config not found for " + tier)));
    }

    public List<TierConfigResponse> getAllTierConfigs() {
        return tierConfigMapper.map(tierConfigRepository.findAll());
    }
}