package interswitch.academy.verve_guard.models.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BulkPermissionRequest(
        @NotEmpty List<@NotBlank String> permissionIds
) {}