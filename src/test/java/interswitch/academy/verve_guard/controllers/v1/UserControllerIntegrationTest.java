package interswitch.academy.verve_guard.controllers.v1;

import interswitch.academy.verve_guard.base.BaseControllerIntegrationTest;
import interswitch.academy.verve_guard.repositories.RoleRepository;
import interswitch.academy.verve_guard.repositories.UserRepository;

import interswitch.academy.verve_guard.entities.Role;
import interswitch.academy.verve_guard.entities.User;
import interswitch.academy.verve_guard.models.enums.UserStatus;
import interswitch.academy.verve_guard.models.request.ChangePasswordRequest;
import interswitch.academy.verve_guard.models.request.CreateUserRequest;
import interswitch.academy.verve_guard.models.request.UpdateUserRequest;
import org.springframework.http.MediaType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("User Controller Integration Tests")
public class UserControllerIntegrationTest extends BaseControllerIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    @DisplayName("should create user successfully as super admin")
    void shouldCreateUserSuccessfullyAsSuperAdmin() throws Exception {
        String token = loginAndGetAccessToken("superadmin@verveguard.com", "Admin123!");
        Role role = roleRepository.findByName("ADMIN").orElseThrow();

        CreateUserRequest request = new CreateUserRequest(
                "John", "Doe", null,
                "john.doe@test.com", "55555555555",
                "Admin123!", role.getId()
        );

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value(request.email()))
                .andExpect(jsonPath("$.data.firstname").value(request.firstname()))
                .andExpect(jsonPath("$.data.lastname").value(request.lastname()));
    }

    @Test
    @DisplayName("should return 403 when merchant tries to create user")
    void shouldReturn403WhenMerchantTriesToCreateUser() throws Exception {
        String token = loginAndGetAccessToken("testmerchant@verveguard.com", "Admin123!");
        Role role = roleRepository.findByName("ADMIN").orElseThrow();

        CreateUserRequest request = new CreateUserRequest(
                "John", "Doe", null,
                "john.doe2@test.com", "55555555556",
                "Admin123!", role.getId()
        );

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        Role role = roleRepository.findByName("ADMIN").orElseThrow();

        CreateUserRequest request = new CreateUserRequest(
                "John", "Doe", null,
                "john.doe3@test.com", "55555555557",
                "Admin123!", role.getId()
        );

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return 400 with invalid request body")
    void shouldReturn400WithInvalidRequestBody() throws Exception {
        String token = loginAndGetAccessToken("superadmin@verveguard.com", "Admin123!");

        CreateUserRequest request = new CreateUserRequest(
                "", "", null, "notanemail", "", "Admin123!", ""
        );

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should get all users successfully")
    void shouldGetAllUsersSuccessfully() throws Exception {
        String token = loginAndGetAccessToken("superadmin@verveguard.com", "Admin123!");

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", bearerToken(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").isNotEmpty());
    }

    @Test
    @DisplayName("should return 403 when merchant tries to get all users")
    void shouldReturn403WhenMerchantTriesToGetAllUsers() throws Exception {
        String token = loginAndGetAccessToken("testmerchant@verveguard.com", "Admin123!");

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", bearerToken(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should get user by id successfully")
    void shouldGetUserByIdSuccessfully() throws Exception {
        String token = loginAndGetAccessToken("superadmin@verveguard.com", "Admin123!");
        User existing = userRepository.findByEmail("testadmin@verveguard.com").orElseThrow();

        mockMvc.perform(get("/api/v1/users/{userId}", existing.getId())
                        .header("Authorization", bearerToken(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(existing.getId()))
                .andExpect(jsonPath("$.data.email").value(existing.getEmail()));
    }

    @Test
    @DisplayName("should return 404 for non existent user")
    void shouldReturn404ForNonExistentUser() throws Exception {
        String token = loginAndGetAccessToken("superadmin@verveguard.com", "Admin123!");

        mockMvc.perform(get("/api/v1/users/{userId}", "NONEXISTENT00000000000000")
                        .header("Authorization", bearerToken(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("should update user successfully")
    void shouldUpdateUserSuccessfully() throws Exception {
        String token = loginAndGetAccessToken("superadmin@verveguard.com", "Admin123!");
        User existing = userRepository.findByEmail("testadmin@verveguard.com").orElseThrow();

        UpdateUserRequest request = new UpdateUserRequest(
                "Updated", "Name", null, existing.getPhone(), existing.getEmail()
        );

        mockMvc.perform(put("/api/v1/users/{userId}", existing.getId())
                        .header("Authorization", bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstname").value(request.firstname()))
                .andExpect(jsonPath("$.data.lastname").value(request.lastname()));
    }

    @Test
    @DisplayName("should change user status successfully")
    void shouldChangeUserStatusSuccessfully() throws Exception {
        String token = loginAndGetAccessToken("superadmin@verveguard.com", "Admin123!");
        User existing = userRepository.findByEmail("testadmin@verveguard.com").orElseThrow();

        mockMvc.perform(patch("/api/v1/users/{userId}/status", existing.getId())
                        .header("Authorization", bearerToken(token))
                        .param("status", UserStatus.SUSPENDED.name()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should change user role successfully")
    void shouldChangeUserRoleSuccessfully() throws Exception {
        String token = loginAndGetAccessToken("superadmin@verveguard.com", "Admin123!");
        User existing = userRepository.findByEmail("testadmin@verveguard.com").orElseThrow();
        Role merchantRole = roleRepository.findByName("MERCHANT").orElseThrow();

        mockMvc.perform(patch("/api/v1/users/{userId}/role", existing.getId())
                        .header("Authorization", bearerToken(token))
                        .param("roleId", merchantRole.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should change password successfully")
    void shouldChangePasswordSuccessfully() throws Exception {
        String token = loginAndGetAccessToken("testadmin@verveguard.com", "Admin123!");
        User existing = userRepository.findByEmail("testadmin@verveguard.com").orElseThrow();

        ChangePasswordRequest request = new ChangePasswordRequest(
                "Admin123!", "NewPassword123!", "NewPassword123!"
        );

        mockMvc.perform(patch("/api/v1/users/{userId}/password", existing.getId())
                        .header("Authorization", bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should return 403 when merchant tries to delete user")
    void shouldReturn403WhenMerchantTriesToDeleteUser() throws Exception {
        String token = loginAndGetAccessToken("testmerchant@verveguard.com", "Admin123!");
        User existing = userRepository.findByEmail("testadmin@verveguard.com").orElseThrow();

        mockMvc.perform(delete("/api/v1/users/{userId}", existing.getId())
                        .header("Authorization", bearerToken(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should delete user successfully as super admin")
    void shouldDeleteUserSuccessfullyAsSuperAdmin() throws Exception {
        String token = loginAndGetAccessToken("superadmin@verveguard.com", "Admin123!");
        User existing = userRepository.findByEmail("testmerchant@verveguard.com").orElseThrow();

        mockMvc.perform(delete("/api/v1/users/{userId}", existing.getId())
                        .header("Authorization", bearerToken(token)))
                .andExpect(status().isNoContent());
    }
}