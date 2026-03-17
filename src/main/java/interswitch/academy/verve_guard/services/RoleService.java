package interswitch.academy.verve_guard.services;

import com.github.f4b6a3.ulid.UlidCreator;
import interswitch.academy.verve_guard.entities.Permission;
import interswitch.academy.verve_guard.entities.Role;
import interswitch.academy.verve_guard.entities.RolePermission;
import interswitch.academy.verve_guard.entities.RolePermissionId;
import interswitch.academy.verve_guard.exceptions.ConflictException;
import interswitch.academy.verve_guard.exceptions.NotFoundException;
import interswitch.academy.verve_guard.mapper.RoleMapper;
import interswitch.academy.verve_guard.models.request.CreateRoleRequest;
import interswitch.academy.verve_guard.models.response.RoleResponse;
import interswitch.academy.verve_guard.repositories.PermissionRepository;
import interswitch.academy.verve_guard.repositories.RolePermissionRepository;
import interswitch.academy.verve_guard.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RoleMapper roleMapper;

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        if (roleRepository.existsByName(request.name())) {
            throw new ConflictException("Role already exists");
        }

        Role role = Role.builder()
                .id(UlidCreator.getUlid().toString())
                .name(request.name())
                .build();

        return roleMapper.map(roleRepository.save(role));
    }

    @Transactional
    public void assignPermission(String roleId, String permissionId) {
        if (!roleRepository.existsById(roleId)) {
            throw new NotFoundException("Role not found");
        }
        if (!permissionRepository.existsById(permissionId)) {
            throw new NotFoundException("Permission not found");
        }
        if (rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId)) {
            throw new ConflictException("Permission already assigned to role");
        }

        RolePermission rolePermission = new RolePermission();
        rolePermission.setId(new RolePermissionId(roleId, permissionId));

        rolePermission.setPermission(new Permission(permissionId));
        rolePermission.setRole(new Role(roleId));

        rolePermissionRepository.save(rolePermission);
    }

    @Transactional
    public void revokePermission(String roleId, String permissionId) {
        if (!rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId)) {
            throw new NotFoundException("Permission not assigned to role");
        }
        rolePermissionRepository.deleteById(new RolePermissionId(roleId, permissionId));
    }

    public RoleResponse getRoleById(String roleId) {
        return roleMapper.map(roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role not found")));
    }

    public List<RoleResponse> getAllRoles() {
        return roleMapper.map(roleRepository.findAll());
    }

    public Page<RoleResponse> getAllRoles(int page, int size, String sortField, Sort.Direction sortDirection) {
        Sort sort = Sort.by(sortDirection, sortField);
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        return roleRepository.findAll(pageable)
                .map(roleMapper::map);
    }

    @Transactional
    public void assignPermissions(String roleId, List<String> permissionIds) {

        if (!roleRepository.existsById(roleId)) {
            throw new NotFoundException("Role not found");
        }

        List<Permission> permissions = permissionRepository.findAllById(permissionIds);
        if (permissions.size() != permissionIds.size()) {
            throw new NotFoundException("One or more permissions not found");
        }

        Set<String> existingPermissionIds =
                rolePermissionRepository.findPermissionIdsByRoleId(roleId);

        List<RolePermission> toAssign = permissions.stream()
                .filter(p -> !existingPermissionIds.contains(p.getId()))
                .map(p -> {
                    RolePermission rp = new RolePermission();
                    rp.setId(new RolePermissionId(roleId, p.getId()));
                    rp.setPermission(new Permission(p.getId()));
                    rp.setRole(new Role(roleId));
                    return rp;
                })
                .toList();

        rolePermissionRepository.saveAll(toAssign);
    }

    @Transactional
    public void revokePermissions(String roleId, List<String> permissionIds) {
        if (!roleRepository.existsById(roleId)) {
            throw new NotFoundException("Role not found");
        }

        List<RolePermissionId> toRevoke = permissionIds.stream()
                .map(permissionId -> new RolePermissionId(roleId, permissionId))
                .toList();

        rolePermissionRepository.deleteAllById(toRevoke);
    }
}