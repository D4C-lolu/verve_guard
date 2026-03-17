package interswitch.academy.verve_guard.controllers.v1;

import interswitch.academy.verve_guard.annotation.ValidSortField;
import interswitch.academy.verve_guard.constants.Permissions;
import interswitch.academy.verve_guard.models.enums.UserStatus;
import interswitch.academy.verve_guard.models.request.ChangePasswordRequest;
import interswitch.academy.verve_guard.models.request.CreateUserRequest;
import interswitch.academy.verve_guard.models.request.UpdateUserRequest;
import interswitch.academy.verve_guard.models.response.UserResponse;
import interswitch.academy.verve_guard.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasAuthority('" + Permissions.USER_CREATE + "')")
    public UserResponse createUser(@RequestBody @Valid CreateUserRequest request) {
        return userService.createUser(request);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.USER_READ + "')")
    public Page<UserResponse> getAllUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @ValidSortField(target = UserResponse.class) @RequestParam(defaultValue = "createdAt") String sortField,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection
    ) {
        return userService.getAllUsers(page, size, sortField, sortDirection);
    }

    @GetMapping("{userId}")
    @PreAuthorize("hasAuthority('" + Permissions.USER_READ + "')")
    public UserResponse getUserById(@PathVariable String userId) {
        return userService.getUserById(userId);
    }

    @PutMapping("{userId}")
    @PreAuthorize("hasAuthority('" + Permissions.USER_UPDATE + "')")
    public UserResponse updateUser(
            @PathVariable String userId,
            @RequestBody @Valid UpdateUserRequest request
    ) {
        return userService.updateUser(userId, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("{userId}/status")
    @PreAuthorize("hasAuthority('" + Permissions.USER_UPDATE + "')")
    public void changeUserStatus(
            @PathVariable String userId,
            @RequestParam UserStatus status
    ) {
        userService.changeUserStatus(userId, status);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("{userId}/role")
    @PreAuthorize("hasAuthority('" + Permissions.USER_UPDATE + "')")
    public void changeUserRole(
            @PathVariable String userId,
            @RequestParam String roleId
    ) {
        userService.changeUserRole(userId, roleId);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("{userId}/password")
    public void changePassword(
            @PathVariable String userId,
            @RequestBody @Valid ChangePasswordRequest request
    ) {
        userService.changePassword(userId, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("{userId}")
    @PreAuthorize("hasAuthority('" + Permissions.USER_DELETE + "')")
    public void deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);
    }
}