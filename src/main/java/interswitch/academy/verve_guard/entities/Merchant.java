package interswitch.academy.verve_guard.entities;

import interswitch.academy.verve_guard.entities.audit.FullAudit;
import interswitch.academy.verve_guard.models.enums.KycStatus;
import interswitch.academy.verve_guard.models.enums.MerchantStatus;
import interswitch.academy.verve_guard.models.enums.MerchantTier;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "merchants")
public class Merchant extends FullAudit {

    @Id
    @Column(nullable = false, updatable = false, length = 26)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "text")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 50)
    private KycStatus kycStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "merchant_status", nullable = false, length = 50)
    private MerchantStatus merchantStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MerchantTier tier;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;

        Class<?> oEffectiveClass = o instanceof HibernateProxy hibernateProxy
                ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy hibernateProxy
                ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();

        if (thisEffectiveClass != oEffectiveClass) return false;
        Merchant that = (Merchant) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy hibernateProxy
                ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Merchant{" +
                "id='" + id + '\'' +
                ", kycStatus=" + kycStatus +
                ", merchantStatus=" + merchantStatus +
                ", tier=" + tier +
                '}';
    }
}
