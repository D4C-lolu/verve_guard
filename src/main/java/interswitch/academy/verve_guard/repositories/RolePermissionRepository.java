package interswitch.academy.verve_guard.repositories;

import interswitch.academy.verve_guard.entities.RolePermission;
import interswitch.academy.verve_guard.entities.RolePermissionId;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;
import java.util.Set;

import static org.hibernate.jpa.HibernateHints.HINT_CACHEABLE;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    @QueryHints(@QueryHint(name = HINT_CACHEABLE, value = "true"))
    List<RolePermission> findByRoleId(String roleId);

    @QueryHints(@QueryHint(name = HINT_CACHEABLE, value = "true"))
    List<RolePermission> findByPermissionId(String permissionId);

    @QueryHints(@QueryHint(name = HINT_CACHEABLE, value = "true"))
    boolean existsByRoleIdAndPermissionId(String roleId, String permissionId);

    @Query("""
        SELECT rp.id.permissionId
        FROM RolePermission rp
        WHERE rp.id.roleId = :roleId
    """)
    Set<String> findPermissionIdsByRoleId(String roleId);
}
