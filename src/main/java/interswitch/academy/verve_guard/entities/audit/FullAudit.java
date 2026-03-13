package interswitch.academy.verve_guard.entities.audit;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Getter
@Setter
@NoArgsConstructor
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class FullAudit {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @CreatedBy
    @Column(updatable = false, length = 26)
    private String createdBy;

    @LastModifiedBy
    @Column(length = 26)
    private String updatedBy;

    @Column
    private OffsetDateTime deletedAt;

    @Column(length = 26)
    private String deletedBy;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete() {
        this.deletedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    @PreUpdate
    protected void preSoftDelete() {
        if (this.deletedAt != null && this.deletedBy == null) {
            this.deletedBy = getCurrentUserId();
        }
    }

    //TODO: Update once userdetails is implemented
    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        // return ((YourUserDetails) auth.getPrincipal()).getId();
        return null;
    }
}


