package interswitch.academy.verve_guard.controllers.v1;

import interswitch.academy.verve_guard.base.BaseControllerIntegrationTest;

import interswitch.academy.verve_guard.entities.User;
import interswitch.academy.verve_guard.models.enums.KycStatus;
import interswitch.academy.verve_guard.models.enums.MerchantStatus;
import interswitch.academy.verve_guard.models.enums.MerchantTier;
import interswitch.academy.verve_guard.models.request.CreateMerchantRequest;
import interswitch.academy.verve_guard.models.request.MerchantSignupRequest;
import interswitch.academy.verve_guard.models.request.UpdateMerchantRequest;
import interswitch.academy.verve_guard.models.response.MerchantResponse;
import interswitch.academy.verve_guard.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Merchant Controller Integration Tests")
public class MerchantControllerIntegrationTest extends BaseControllerIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    private String superAdminToken;
    private String merchantToken;
    private User merchantUser;

    @BeforeEach
    void setup() throws Exception {
        superAdminToken = loginAndGetAccessToken("superadmin@verveguard.com", "Admin123!");
        merchantToken   = loginAndGetAccessToken("testmerchant@verveguard.com", "Admin123!");
        merchantUser    = userRepository.findByEmail("testmerchant@verveguard.com").orElseThrow();
    }

    private MerchantResponse createTestMerchant() throws Exception {
        CreateMerchantRequest request = new CreateMerchantRequest(merchantUser.getId(), "123 Test Street");
        MvcResult result = mockMvc.perform(post("/api/v1/merchants")
                        .header("Authorization", bearerToken(superAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(
                objectMapper.readTree(result.getResponse().getContentAsString()).get("data").toString(),
                MerchantResponse.class
        );
    }

    @Test
    @DisplayName("should create merchant successfully as super admin")
    void shouldCreateMerchantSuccessfully() throws Exception {
        CreateMerchantRequest request = new CreateMerchantRequest(merchantUser.getId(), "123 Test Street");

        mockMvc.perform(post("/api/v1/merchants")
                        .header("Authorization", bearerToken(superAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.address").value(request.address()))
                .andExpect(jsonPath("$.data.kycStatus").value(KycStatus.PENDING.name()))
                .andExpect(jsonPath("$.data.merchantStatus").value(MerchantStatus.INACTIVE.name()))
                .andExpect(jsonPath("$.data.tier").value(MerchantTier.TIER_1.name()));
    }

    @Test
    @DisplayName("should return 403 when merchant tries to create merchant")
    void shouldReturn403WhenMerchantTriesToCreateMerchant() throws Exception {
        CreateMerchantRequest request = new CreateMerchantRequest(merchantUser.getId(), "123 Test Street");

        mockMvc.perform(post("/api/v1/merchants")
                        .header("Authorization", bearerToken(merchantToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 409 when user already has merchant account")
    void shouldReturn409WhenUserAlreadyHasMerchantAccount() throws Exception {
        CreateMerchantRequest request = new CreateMerchantRequest(merchantUser.getId(), "123 Test Street");
        createTestMerchant();

        mockMvc.perform(post("/api/v1/merchants")
                        .header("Authorization", bearerToken(superAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("should get all merchants successfully")
    void shouldGetAllMerchantsSuccessfully() throws Exception {
        createTestMerchant();

        mockMvc.perform(get("/api/v1/merchants")
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").isNotEmpty());
    }

    @Test
    @DisplayName("should get merchant by id successfully")
    void shouldGetMerchantByIdSuccessfully() throws Exception {
        MerchantResponse created = createTestMerchant();

        mockMvc.perform(get("/api/v1/merchants/{merchantId}", created.id())
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(created.id()));
    }

    @Test
    @DisplayName("should return 404 for non existent merchant")
    void shouldReturn404ForNonExistentMerchant() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/{merchantId}", "NONEXISTENT00000000000000")
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("should update merchant successfully")
    void shouldUpdateMerchantSuccessfully() throws Exception {
        MerchantResponse created = createTestMerchant();
        UpdateMerchantRequest request = new UpdateMerchantRequest("456 Updated Street");

        mockMvc.perform(put("/api/v1/merchants/{merchantId}", created.id())
                        .header("Authorization", bearerToken(superAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.address").value(request.address()));
    }

    @Test
    @DisplayName("should update kyc status successfully")
    void shouldUpdateKycStatusSuccessfully() throws Exception {
        MerchantResponse created = createTestMerchant();

        mockMvc.perform(patch("/api/v1/merchants/{merchantId}/kyc", created.id())
                        .header("Authorization", bearerToken(superAdminToken))
                        .param("kycStatus", KycStatus.APPROVED.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kycStatus").value(KycStatus.APPROVED.name()))
                .andExpect(jsonPath("$.data.merchantStatus").value(MerchantStatus.ACTIVE.name()));
    }

    @Test
    @DisplayName("should upgrade tier successfully")
    void shouldUpgradeTierSuccessfully() throws Exception {
        MerchantResponse created = createTestMerchant();
        mockMvc.perform(patch("/api/v1/merchants/{merchantId}/kyc", created.id())
                .header("Authorization", bearerToken(superAdminToken))
                .param("kycStatus", KycStatus.APPROVED.name()));

        mockMvc.perform(patch("/api/v1/merchants/{merchantId}/tier/upgrade", created.id())
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tier").value(MerchantTier.TIER_2.name()));
    }

    @Test
    @DisplayName("should downgrade tier successfully")
    void shouldDowngradeTierSuccessfully() throws Exception {
        MerchantResponse created = createTestMerchant();
        mockMvc.perform(patch("/api/v1/merchants/{merchantId}/kyc", created.id())
                .header("Authorization", bearerToken(superAdminToken))
                .param("kycStatus", KycStatus.APPROVED.name()));
        mockMvc.perform(patch("/api/v1/merchants/{merchantId}/tier/upgrade", created.id())
                .header("Authorization", bearerToken(superAdminToken)));

        mockMvc.perform(patch("/api/v1/merchants/{merchantId}/tier/downgrade", created.id())
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tier").value(MerchantTier.TIER_1.name()));
    }

    @Test
    @DisplayName("should update merchant status successfully")
    void shouldUpdateMerchantStatusSuccessfully() throws Exception {
        MerchantResponse created = createTestMerchant();
        mockMvc.perform(patch("/api/v1/merchants/{merchantId}/kyc", created.id())
                .header("Authorization", bearerToken(superAdminToken))
                .param("kycStatus", KycStatus.APPROVED.name()));

        mockMvc.perform(patch("/api/v1/merchants/{merchantId}/status", created.id())
                        .header("Authorization", bearerToken(superAdminToken))
                        .param("status", MerchantStatus.SUSPENDED.name()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should delete merchant successfully as super admin")
    void shouldDeleteMerchantSuccessfully() throws Exception {
        MerchantResponse created = createTestMerchant();

        mockMvc.perform(delete("/api/v1/merchants/{merchantId}", created.id())
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should return 403 when merchant tries to delete merchant")
    void shouldReturn403WhenMerchantTriesToDeleteMerchant() throws Exception {
        MerchantResponse created = createTestMerchant();

        mockMvc.perform(delete("/api/v1/merchants/{merchantId}", created.id())
                        .header("Authorization", bearerToken(merchantToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/merchants"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should register new user as merchant successfully")
    void shouldRegisterNewUserAsMerchantSuccessfully() throws Exception {
        MerchantSignupRequest request = new MerchantSignupRequest(
                "New", "Merchant", null,
                "newmerchant@test.com", "88888888888",
                "Admin123!", "789 New Street"
        );

        mockMvc.perform(post("/api/v1/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.kycStatus").value(KycStatus.PENDING.name()))
                .andExpect(jsonPath("$.data.merchantStatus").value(MerchantStatus.INACTIVE.name()))
                .andExpect(jsonPath("$.data.tier").value(MerchantTier.TIER_1.name()));
    }

    @Test
    @DisplayName("should return 409 when registering new user with duplicate email")
    void shouldReturn409WhenRegisteringNewUserWithDuplicateEmail() throws Exception {
        User existing = userRepository.findByEmail("testmerchant@verveguard.com").orElseThrow();
        MerchantSignupRequest request = new MerchantSignupRequest(
                "New", "Merchant", null,
                existing.getEmail(), "88888888888",
                "Admin123!", "789 New Street"
        );

        mockMvc.perform(post("/api/v1/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("should return 400 with invalid register request")
    void shouldReturn400WithInvalidRegisterRequest() throws Exception {
        MerchantSignupRequest request = new MerchantSignupRequest(
                "", "", null, "notanemail", "", "", null
        );

        mockMvc.perform(post("/api/v1/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should self register existing user as merchant successfully")
    void shouldSelfRegisterExistingUserAsMerchantSuccessfully() throws Exception {
        mockMvc.perform(post("/api/v1/merchants/self-register")
                        .header("Authorization", bearerToken(merchantToken))
                        .param("address", "123 Self Street"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.kycStatus").value(KycStatus.PENDING.name()))
                .andExpect(jsonPath("$.data.merchantStatus").value(MerchantStatus.INACTIVE.name()));
    }

    @Test
    @DisplayName("should return 401 when self registering without token")
    void shouldReturn401WhenSelfRegisteringWithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/merchants/self-register")
                        .param("address", "123 Self Street"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return 409 when self registering user already has merchant account")
    void shouldReturn409WhenSelfRegisteringUserAlreadyHasMerchantAccount() throws Exception {
        mockMvc.perform(post("/api/v1/merchants/self-register")
                .header("Authorization", bearerToken(merchantToken))
                .param("address", "123 Self Street"));

        mockMvc.perform(post("/api/v1/merchants/self-register")
                        .header("Authorization", bearerToken(merchantToken))
                        .param("address", "123 Self Street"))
                .andExpect(status().isConflict());
    }
}