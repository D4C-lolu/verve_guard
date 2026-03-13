package interswitch.academy.verve_guard.repositories;

import interswitch.academy.verve_guard.entities.RolePermission;
import interswitch.academy.verve_guard.entities.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    List<RolePermission> findByRoleId(String roleId);

    List<RolePermission> findByPermissionId(String permissionId);

    boolean existsByRoleIdAndPermissionId(String roleId, String permissionId);
}
