package interswitch.academy.verve_guard.controllers.v1;

import interswitch.academy.verve_guard.base.BaseControllerIntegrationTest;

import interswitch.academy.verve_guard.models.enums.*;
import interswitch.academy.verve_guard.models.request.CreateCardRequest;
import interswitch.academy.verve_guard.models.request.CreateMyCardRequest;
import interswitch.academy.verve_guard.models.response.CardResponse;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Card Controller Integration Tests")
public class CardControllerIntegrationTest extends BaseControllerIntegrationTest {

    @BeforeEach
    void setup() throws Exception {
        superAdminToken = loginAndGetAccessToken("superadmin@verveguard.com", "Admin123!");
        merchantToken = loginAndGetAccessToken("demo.merchant@verveguard.com", "Admin123!");
    }

    private CardResponse createTestCard(String cardNumber) throws Exception {
        CreateCardRequest request = new CreateCardRequest(
                "01JACCTS0000000000000001AA",
                cardNumber,
                CardType.VIRTUAL,
                CardScheme.VISA,
                12, 3028
        );
        MvcResult result = mockMvc.perform(post("/api/v1/cards")
                        .header("Authorization", bearerToken(superAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(
                objectMapper.readTree(result.getResponse().getContentAsString()).get("data").toString(),
                CardResponse.class
        );
    }

    @Test
    @DisplayName("should create card successfully as super admin")
    void shouldCreateCardSuccessfully() throws Exception {
        CreateCardRequest request = new CreateCardRequest(
                "01JACCTS0000000000000001AA",
                "4111222233334444",
                CardType.VIRTUAL,
                CardScheme.VISA,
                12, 3028
        );

        mockMvc.perform(post("/api/v1/cards")
                        .header("Authorization", bearerToken(superAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accountId").value(request.accountId()))
                .andExpect(jsonPath("$.data.cardType").value(request.cardType().name()))
                .andExpect(jsonPath("$.data.scheme").value(request.scheme().name()))
                .andExpect(jsonPath("$.data.cardStatus").value(CardStatus.ACTIVE.name()));
    }

    @Test
    @DisplayName("should return 403 when merchant tries to create card via admin endpoint")
    void shouldReturn403WhenMerchantTriesToCreateCardViaAdminEndpoint() throws Exception {
        CreateCardRequest request = new CreateCardRequest(
                "01JACCTS0000000000000001AA",
                "4111222233335555",
                CardType.VIRTUAL,
                CardScheme.VISA,
                12, 3028
        );

        mockMvc.perform(post("/api/v1/cards")
                        .header("Authorization", bearerToken(merchantToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should create card for self successfully as merchant")
    void shouldCreateCardForSelfSuccessfully() throws Exception {
        CreateMyCardRequest request = new CreateMyCardRequest(
                "01JACCTS0000000000000001AA",
                "4111222233336666",
                CardType.VIRTUAL,
                CardScheme.VISA,
                12, 3028
        );

        mockMvc.perform(post("/api/v1/cards/me")
                        .header("Authorization", bearerToken(merchantToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accountId").value(request.accountId()))
                .andExpect(jsonPath("$.data.cardStatus").value(CardStatus.ACTIVE.name()));
    }

    @Test
    @DisplayName("should return 403 when admin tries to create card via merchant endpoint")
    void shouldReturn403WhenAdminTriesToCreateCardViaMerchantEndpoint() throws Exception {
        CreateMyCardRequest request = new CreateMyCardRequest(
                "01JACCTS0000000000000001AA",
                "4111222233337777",
                CardType.VIRTUAL,
                CardScheme.VISA,
                12, 3028
        );

        mockMvc.perform(post("/api/v1/cards/me")
                        .header("Authorization", bearerToken(superAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should get card by id successfully")
    void shouldGetCardByIdSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/cards/{cardId}", "01JCARDS0000000000000001AA")
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("01JCARDS0000000000000001AA"))
                .andExpect(jsonPath("$.data.accountId").value("01JACCTS0000000000000001AA"));
    }

    @Test
    @DisplayName("should return 404 for non existent card")
    void shouldReturn404ForNonExistentCard() throws Exception {
        mockMvc.perform(get("/api/v1/cards/{cardId}", "NONEXISTENT00000000000000")
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("should get cards by account successfully")
    void shouldGetCardsByAccountSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/cards/account/{accountId}", "01JACCTS0000000000000001AA")
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").isNotEmpty());
    }

    @Test
    @DisplayName("should update card status successfully")
    void shouldUpdateCardStatusSuccessfully() throws Exception {
        CardResponse created = createTestCard("4111222233338888");

        mockMvc.perform(patch("/api/v1/cards/{cardId}/status", created.id())
                        .header("Authorization", bearerToken(superAdminToken))
                        .param("status", CardStatus.BLOCKED.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cardStatus").value(CardStatus.BLOCKED.name()));
    }

    @Test
    @DisplayName("should block card successfully as admin")
    void shouldBlockCardSuccessfullyAsAdmin() throws Exception {
        CardResponse created = createTestCard("4111222233339999");

        mockMvc.perform(patch("/api/v1/cards/{cardId}/block", created.id())
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should block card for self successfully as merchant")
    void shouldBlockCardForSelfSuccessfully() throws Exception {
        mockMvc.perform(patch("/api/v1/cards/{cardId}/block/me", "01JCARDS0000000000000001AA")
                        .header("Authorization", bearerToken(merchantToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should return 403 when merchant tries to block card they dont own")
    void shouldReturn403WhenMerchantTriesToBlockCardTheyDontOwn() throws Exception {
        String otherMerchantToken = loginAndGetAccessToken("testmerchant@verveguard.com", "Admin123!");

        mockMvc.perform(patch("/api/v1/cards/{cardId}/block/me", "01JCARDS0000000000000001AA")
                        .header("Authorization", bearerToken(otherMerchantToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should delete card successfully")
    void shouldDeleteCardSuccessfully() throws Exception {
        CardResponse created = createTestCard("4111222200001111");

        mockMvc.perform(delete("/api/v1/cards/{cardId}", created.id())
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/cards/{cardId}", "01JCARDS0000000000000001AA"))
                .andExpect(status().isUnauthorized());
    }
}