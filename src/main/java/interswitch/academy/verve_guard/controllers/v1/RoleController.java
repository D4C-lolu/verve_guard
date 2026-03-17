package interswitch.academy.verve_guard.controllers.v1;

import interswitch.academy.verve_guard.annotation.ValidSortField;
import interswitch.academy.verve_guard.constants.Permissions;
import interswitch.academy.verve_guard.models.request.*;
import interswitch.academy.verve_guard.models.response.RoleResponse;
import interswitch.academy.verve_guard.services.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasAuthority('" + Permissions.ROLE_CREATE + "')")
    public RoleResponse createRole(@RequestBody @Valid CreateRoleRequest request) {
        return roleService.createRole(request);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.ROLE_READ + "')")
    public Page<RoleResponse> getAllRoles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @ValidSortField(target = RoleResponse.class) @RequestParam(defaultValue = "name") String sortField,
            @RequestParam(defaultValue = "ASC") Sort.Direction sortDirection
    ) {
        return roleService.getAllRoles(page, size, sortField, sortDirection);
    }

    @GetMapping("{roleId}")
    @PreAuthorize("hasAuthority('" + Permissions.ROLE_READ + "')")
    public RoleResponse getRoleById(@PathVariable String roleId) {
        return roleService.getRoleById(roleId);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("{roleId}/permissions")
    @PreAuthorize("hasAuthority('" + Permissions.PERMISSION_ASSIGN + "')")
    public void assignPermissions(
            @PathVariable String roleId,
            @RequestBody @Valid BulkPermissionRequest request
    ) {
        roleService.assignPermissions(roleId, request.permissionIds());
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("{roleId}/permissions")
    @PreAuthorize("hasAuthority('" + Permissions.PERMISSION_ASSIGN + "')")
    public void revokePermissions(
            @PathVariable String roleId,
            @RequestBody @Valid BulkPermissionRequest request
    ) {
        roleService.revokePermissions(roleId, request.permissionIds());
    }
}