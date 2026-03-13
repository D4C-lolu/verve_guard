package interswitch.academy.verve_guard.entities;

import interswitch.academy.verve_guard.entities.audit.MutableAudit;
import interswitch.academy.verve_guard.models.enums.TransferStatus;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.hibernate.proxy.HibernateProxy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transfers")
public class Transfer extends MutableAudit {

    @Id
    @Column(nullable = false, updatable = false, length = 26)
    private String id;

    @Column(nullable = false, unique = true, length = 100)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id", nullable = false)
    private Account fromAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id", nullable = false)
    private Account toAccount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_status", nullable = false, length = 50)
    private TransferStatus transferStatus;

    @Column(columnDefinition = "text")
    private String description;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @OneToMany(mappedBy = "transfer", fetch = FetchType.LAZY)
    private List<Transaction> transactions;

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
        Transfer that = (Transfer) o;
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
        return "Transfer{" +
                "id='" + id + '\'' +
                ", reference='" + reference + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", transferStatus=" + transferStatus +
                '}';
    }
}
