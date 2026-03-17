package interswitch.academy.verve_guard.repositories;

import interswitch.academy.verve_guard.entities.Merchant;
import interswitch.academy.verve_guard.models.enums.KycStatus;
import interswitch.academy.verve_guard.models.enums.MerchantStatus;
import interswitch.academy.verve_guard.models.projections.MerchantProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, String> {

    Optional<Merchant> findByUserId(String userId);

    List<Merchant> findAllByDeletedAtIsNull();

    List<Merchant> findAllByMerchantStatus(MerchantStatus merchantStatus);

    List<Merchant> findAllByKycStatus(KycStatus kycStatus);

    boolean existsByUserId(String userId);

    @Query("SELECT m.id as id, m.address as address, m.kycStatus as kycStatus, " +
            "m.merchantStatus as merchantStatus, m.tier as tier, " +
            "m.createdAt as createdAt, m.updatedAt as updatedAt, " +
            "u.id as userId, u.firstname as userFirstname, u.lastname as userLastname, " +
            "u.email as userEmail, u.phone as userPhone, u.userStatus as userStatus " +
            "FROM Merchant m JOIN m.user u WHERE m.deletedAt IS NULL")
    Page<MerchantProjection> findAllMerchants(Pageable pageable);

    @Query("SELECT m.id as id, m.address as address, m.kycStatus as kycStatus, " +
            "m.merchantStatus as merchantStatus, m.tier as tier, " +
            "m.createdAt as createdAt, m.updatedAt as updatedAt, " +
            "u.id as userId, u.firstname as userFirstname, u.lastname as userLastname, " +
            "u.email as userEmail, u.phone as userPhone, u.userStatus as userStatus " +
            "FROM Merchant m JOIN m.user u WHERE m.id = :merchantId AND m.deletedAt IS NULL")
    Optional<MerchantProjection> findMerchantById(@Param("merchantId") String merchantId);

    Page<MerchantProjection> findAllByMerchantStatusAndDeletedAtIsNull(MerchantStatus status, Pageable pageable);

    Page<MerchantProjection> findAllByKycStatusAndDeletedAtIsNull(KycStatus kycStatus, Pageable pageable);
}
