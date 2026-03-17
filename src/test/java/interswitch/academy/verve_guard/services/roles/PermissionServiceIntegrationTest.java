package interswitch.academy.verve_guard.services.roles;

import interswitch.academy.verve_guard.base.BaseIntegrationTest;
import interswitch.academy.verve_guard.entities.Permission;
import interswitch.academy.verve_guard.entities.User;
import interswitch.academy.verve_guard.exceptions.ConflictException;
import interswitch.academy.verve_guard.exceptions.NotFoundException;
import interswitch.academy.verve_guard.models.request.CreatePermissionRequest;
import interswitch.academy.verve_guard.models.response.PermissionResponse;
import interswitch.academy.verve_guard.repositories.PermissionRepository;
import interswitch.academy.verve_guard.repositories.UserRepository;
import interswitch.academy.verve_guard.security.UserPrincipal;
import interswitch.academy.verve_guard.services.PermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;


@DisplayName("Permission Service Integration Tests")
public class PermissionServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setupSecurityContext() {
        User superAdmin = userRepository.findByEmail("superadmin@verveguard.com").orElseThrow();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                new UserPrincipal(superAdmin), null, List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("should create permission successfully")
    void shouldCreatePermissionSuccessfully() {
        CreatePermissionRequest request = new CreatePermissionRequest("new:permission", "A new permission");

        PermissionResponse response = permissionService.createPermission(request);

        assertThat(response.id()).isNotBlank();
        assertThat(response.name()).isEqualTo(request.name());
        assertThat(response.description()).isEqualTo(request.description());
    }

    @Test
    @DisplayName("should fail create permission with duplicate name")
    void shouldFailCreatePermissionWithDuplicateName() {
        Permission existing = permissionRepository.findByName("user:read").orElseThrow();
        CreatePermissionRequest request = new CreatePermissionRequest(existing.getName(), "duplicate");

        assertThatThrownBy(() -> permissionService.createPermission(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Permission already exists");
    }

    @Test
    @DisplayName("should get permission by id successfully")
    void shouldGetPermissionByIdSuccessfully() {
        Permission existing = permissionRepository.findByName("user:read").orElseThrow();

        PermissionResponse response = permissionService.getPermissionById(existing.getId());

        assertThat(response.id()).isEqualTo(existing.getId());
        assertThat(response.name()).isEqualTo(existing.getName());
        assertThat(response.description()).isEqualTo(existing.getDescription());
    }

    @Test
    @DisplayName("should fail get permission with non existent id")
    void shouldFailGetPermissionWithNonExistentId() {
        assertThatThrownBy(() -> permissionService.getPermissionById("NONEXISTENT00000000000000"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Permission not found");
    }

    @Test
    @DisplayName("should get all permissions successfully")
    void shouldGetAllPermissionsSuccessfully() {
        List<PermissionResponse> permissions = permissionService.getAllPermissions();

        assertFalse(permissions.isEmpty());
        assertThat(permissions.size()).isGreaterThanOrEqualTo(1);
    }
}