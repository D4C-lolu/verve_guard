package interswitch.academy.verve_guard.services;

import com.github.f4b6a3.ulid.UlidCreator;
import interswitch.academy.verve_guard.entities.Merchant;
import interswitch.academy.verve_guard.entities.Role;
import interswitch.academy.verve_guard.exceptions.BadRequestException;
import interswitch.academy.verve_guard.exceptions.ConflictException;
import interswitch.academy.verve_guard.exceptions.NotFoundException;
import interswitch.academy.verve_guard.mapper.MerchantMapper;
import interswitch.academy.verve_guard.models.enums.KycStatus;
import interswitch.academy.verve_guard.models.enums.MerchantStatus;
import interswitch.academy.verve_guard.models.enums.MerchantTier;
import interswitch.academy.verve_guard.models.request.CreateMerchantRequest;
import interswitch.academy.verve_guard.models.request.CreateUserRequest;
import interswitch.academy.verve_guard.models.request.MerchantSignupRequest;
import interswitch.academy.verve_guard.models.request.UpdateMerchantRequest;
import interswitch.academy.verve_guard.models.response.MerchantResponse;
import interswitch.academy.verve_guard.models.response.UserResponse;
import interswitch.academy.verve_guard.repositories.MerchantRepository;
import interswitch.academy.verve_guard.repositories.RoleRepository;
import interswitch.academy.verve_guard.repositories.TierConfigRepository;
import interswitch.academy.verve_guard.repositories.UserRepository;
import interswitch.academy.verve_guard.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;
    private final TierConfigRepository tierConfigRepository;
    private final MerchantMapper merchantMapper;
    private final RoleRepository roleRepository;
    private final UserService userService;

    @Transactional
    public MerchantResponse createMerchant(CreateMerchantRequest request) {
        if (merchantRepository.existsByUserId(request.userId())) {
            throw new ConflictException("User already has a merchant account");
        }

        if (!userRepository.existsById(request.userId())) {
            throw new NotFoundException("User not found");
        }

        if (!tierConfigRepository.existsByTier(MerchantTier.TIER_1)) {
            throw new NotFoundException("Tier configuration not found for TIER_1");
        }

        Merchant merchant = Merchant.builder()
                .id(UlidCreator.getUlid().toString())
                .user(userRepository.getReferenceById(request.userId()))
                .address(request.address())
                .kycStatus(KycStatus.PENDING)
                .merchantStatus(MerchantStatus.INACTIVE)
                .tier(MerchantTier.TIER_1)
                .build();

        merchantRepository.save(merchant);
        return merchantMapper.map(merchantRepository.findMerchantById(merchant.getId()).orElseThrow());
    }

    @Transactional
    public MerchantResponse selfRegisterExistingUser(String address) {
        String userId = SecurityUtil.getCurrentUserId();

        if (merchantRepository.existsByUserId(userId)) {
            throw new ConflictException("User already has a merchant account");
        }

        CreateMerchantRequest request = new CreateMerchantRequest(userId, address);
        return createMerchant(request);
    }

    @Transactional
    public MerchantResponse registerNewUserAsMerchant(MerchantSignupRequest request) {
        Role merchantRole = roleRepository.findByName("MERCHANT")
                .orElseThrow(() -> new NotFoundException("Merchant role not found"));

        CreateUserRequest createUserRequest = new CreateUserRequest(
                request.firstname(),
                request.lastname(),
                request.othername(),
                request.email(),
                request.phone(),
                request.password(),
                merchantRole.getId()
        );

        UserResponse newUser = userService.createUser(createUserRequest);

        CreateMerchantRequest createMerchantRequest = new CreateMerchantRequest(
                newUser.id(), request.address()
        );

        return createMerchant(createMerchantRequest);
    }

    @Transactional
    public MerchantResponse updateMerchant(String merchantId, UpdateMerchantRequest request) {
        Merchant merchant = findActiveMerchantById(merchantId);

        if (request.address() != null) merchant.setAddress(request.address());

        merchantRepository.save(merchant);
        return merchantMapper.map(merchantRepository.findMerchantById(merchantId).orElseThrow());
    }

    @Transactional
    public MerchantResponse updateKycStatus(String merchantId, KycStatus kycStatus) {
        Merchant merchant = findActiveMerchantById(merchantId);
        KycStatus previous = merchant.getKycStatus();

        if (previous == KycStatus.APPROVED) {
            throw new BadRequestException("Merchant KYC is already approved");
        }

        merchant.setKycStatus(kycStatus);

        if (kycStatus == KycStatus.APPROVED) {
            merchant.setMerchantStatus(MerchantStatus.ACTIVE);
        }

        if (kycStatus == KycStatus.REJECTED) {
            merchant.setMerchantStatus(MerchantStatus.INACTIVE);
        }

        merchantRepository.save(merchant);
        return merchantMapper.map(merchantRepository.findMerchantById(merchantId).orElseThrow());
    }

    @Transactional
    public MerchantResponse upgradeTier(String merchantId) {
        Merchant merchant = findActiveMerchantById(merchantId);

        if (merchant.getKycStatus() != KycStatus.APPROVED) {
            throw new BadRequestException("Merchant must be KYC approved before upgrading tier");
        }

        MerchantTier currentTier = merchant.getTier();
        MerchantTier nextTier = switch (currentTier) {
            case TIER_1 -> MerchantTier.TIER_2;
            case TIER_2 -> MerchantTier.TIER_3;
            case TIER_3 -> throw new BadRequestException("Merchant is already on the highest tier");
        };

        if (!tierConfigRepository.existsByTier(nextTier)) {
            throw new NotFoundException("Tier configuration not found for " + nextTier);
        }

        merchant.setTier(nextTier);
        merchantRepository.save(merchant);
        return merchantMapper.map(merchantRepository.findMerchantById(merchantId).orElseThrow());
    }

    @Transactional
    public MerchantResponse downgradeTier(String merchantId) {
        Merchant merchant = findActiveMerchantById(merchantId);

        MerchantTier currentTier = merchant.getTier();
        MerchantTier previousTier = switch (currentTier) {
            case TIER_3 -> MerchantTier.TIER_2;
            case TIER_2 -> MerchantTier.TIER_1;
            case TIER_1 -> throw new BadRequestException("Merchant is already on the lowest tier");
        };

        merchant.setTier(previousTier);
        merchantRepository.save(merchant);
        return merchantMapper.map(merchantRepository.findMerchantById(merchantId).orElseThrow());
    }

    @Transactional
    public MerchantResponse updateMerchantStatus(String merchantId, MerchantStatus status) {
        Merchant merchant = findActiveMerchantById(merchantId);

        if (merchant.getKycStatus() != KycStatus.APPROVED && status == MerchantStatus.ACTIVE) {
            throw new BadRequestException("Merchant must be KYC approved before activation");
        }

        merchant.setMerchantStatus(status);
        merchantRepository.save(merchant);
        return merchantMapper.map(merchantRepository.findMerchantById(merchantId).orElseThrow());
    }

    @Transactional
    public void deleteMerchant(String merchantId) {
        Merchant merchant = findActiveMerchantById(merchantId);
        merchant.softDelete();
        merchantRepository.save(merchant);
    }

    public MerchantResponse getMerchantById(String merchantId) {
        return merchantMapper.map(
                merchantRepository.findMerchantById(merchantId)
                        .orElseThrow(() -> new NotFoundException("Merchant not found"))
        );
    }

    public Page<MerchantResponse> getAllMerchants(int page, int size, String sortField, Sort.Direction sortDirection) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(sortDirection, sortField));
        return merchantRepository.findAllMerchants(pageable)
                .map(merchantMapper::map);
    }

    public Page<MerchantResponse> getMerchantsByStatus(MerchantStatus status, int page, int size, String sortField, Sort.Direction sortDirection) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(sortDirection, sortField));
        return merchantRepository.findAllByMerchantStatusAndDeletedAtIsNull(status, pageable)
                .map(merchantMapper::map);
    }

    public Page<MerchantResponse> getMerchantsByKycStatus(KycStatus kycStatus, int page, int size, String sortField, Sort.Direction sortDirection) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(sortDirection, sortField));
        return merchantRepository.findAllByKycStatusAndDeletedAtIsNull(kycStatus, pageable)
                .map(merchantMapper::map);
    }

    private Merchant findActiveMerchantById(String merchantId) {
        return merchantRepository.findById(merchantId)
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> new NotFoundException("Merchant not found"));
    }
}