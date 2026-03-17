package interswitch.academy.verve_guard.models.response;

import java.util.List;

public record RoleResponse(
        String id,
        String name,
        List<PermissionResponse> permissions
) {}
