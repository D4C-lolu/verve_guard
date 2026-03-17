package interswitch.academy.verve_guard.models.response;

import interswitch.academy.verve_guard.models.enums.UserStatus;

import java.time.OffsetDateTime;

public record UserResponse(
        String id,
        String firstname,
        String lastname,
        String othername,
        String email,
        String phone,
        String role,
        UserStatus userStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}

