package interswitch.academy.verve_guard.controllers.v1;

import interswitch.academy.verve_guard.constants.Permissions;
import interswitch.academy.verve_guard.models.request.CreatePermissionRequest;
import interswitch.academy.verve_guard.models.response.PermissionResponse;
import interswitch.academy.verve_guard.services.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasAuthority('" + Permissions.ROLE_CREATE + "')")
    public PermissionResponse createPermission(@RequestBody @Valid CreatePermissionRequest request) {
        return permissionService.createPermission(request);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.PERMISSION_READ + "')")
    public List<PermissionResponse> getAllPermissions() {
        return permissionService.getAllPermissions();
    }

    @GetMapping("{permissionId}")
    @PreAuthorize("hasAuthority('" + Permissions.PERMISSION_READ + "')")
    public PermissionResponse getPermissionById(@PathVariable String permissionId) {
        return permissionService.getPermissionById(permissionId);
    }
}