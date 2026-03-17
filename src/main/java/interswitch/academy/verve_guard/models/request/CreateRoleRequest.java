package interswitch.academy.verve_guard.models.request;

import jakarta.validation.constraints.NotBlank;

public record CreateRoleRequest(@NotBlank String name) {}

