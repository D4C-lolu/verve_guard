package interswitch.academy.verve_guard.controllers.v1;

import interswitch.academy.verve_guard.base.BaseControllerIntegrationTest;

import interswitch.academy.verve_guard.models.enums.TransferStatus;
import interswitch.academy.verve_guard.models.request.TransferRequest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Transfer Controller Integration Tests")
public class TransferControllerIntegrationTest extends BaseControllerIntegrationTest {

    private String superAdminToken;
    private String merchantToken;

    @BeforeEach
    void setup() throws Exception {
        superAdminToken = loginAndGetAccessToken("superadmin@verveguard.com", "Admin123!");
        merchantToken   = loginAndGetAccessToken("demo.merchant@verveguard.com", "Admin123!");
    }

    private TransferRequest buildRequest(String reference) {
        return new TransferRequest(
                reference,
                "01JACCTS0000000000000001AA",
                "01JACCTS0000000000000002BB",
                new BigDecimal("1000.00"),
                "NGN", "Test transfer", "4111111111111111"
        );
    }

    @Test
    @DisplayName("should transfer successfully as super admin")
    void shouldTransferSuccessfully() throws Exception {
        TransferRequest request = buildRequest("REF-CTRL-001");

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", bearerToken(superAdminToken))
                        .with(r -> { r.setRemoteAddr(uniqueIp()); return r; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reference").value(request.reference()))
                .andExpect(jsonPath("$.data.transferStatus").value(TransferStatus.SUCCESS.name()));
    }

    @Test
    @DisplayName("should return 403 when merchant tries to transfer via admin endpoint")
    void shouldReturn403WhenMerchantTriesToTransferViaAdminEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", bearerToken(merchantToken))
                        .with(r -> { r.setRemoteAddr(uniqueIp()); return r; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("REF-CTRL-002"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should transfer for self successfully as merchant")
    void shouldTransferForSelfSuccessfully() throws Exception {
        mockMvc.perform(post("/api/v1/transfers/me")
                        .header("Authorization", bearerToken(merchantToken))
                        .with(r -> { r.setRemoteAddr(uniqueIp()); return r; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("REF-CTRL-003"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.transferStatus").value(TransferStatus.SUCCESS.name()));
    }

    @Test
    @DisplayName("should return 409 with duplicate reference")
    void shouldReturn409WithDuplicateReference() throws Exception {
        TransferRequest request = buildRequest("REF-CTRL-004");
        mockMvc.perform(post("/api/v1/transfers")
                .header("Authorization", bearerToken(superAdminToken))
                .with(r -> { r.setRemoteAddr(uniqueIp()); return r; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", bearerToken(superAdminToken))
                        .with(r -> { r.setRemoteAddr(uniqueIp()); return r; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("should return 400 with insufficient funds")
    void shouldReturn400WithInsufficientFunds() throws Exception {
        TransferRequest request = new TransferRequest(
                "REF-CTRL-005",
                "01JACCTS0000000000000001AA",
                "01JACCTS0000000000000002BB",
                new BigDecimal("999999999999.00"),
                "NGN", "Test", "4111111111111111"
        );

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", bearerToken(superAdminToken))
                        .with(r -> { r.setRemoteAddr(uniqueIp()); return r; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should get transfer by id successfully")
    void shouldGetTransferByIdSuccessfully() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", bearerToken(superAdminToken))
                        .with(r -> { r.setRemoteAddr(uniqueIp()); return r; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("REF-CTRL-006"))))
                .andReturn();

        String transferId = objectMapper.readTree(
                result.getResponse().getContentAsString()).get("data").get("id").asText();

        mockMvc.perform(get("/api/v1/transfers/{transferId}", transferId)
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(transferId));
    }

    @Test
    @DisplayName("should return 404 for non existent transfer")
    void shouldReturn404ForNonExistentTransfer() throws Exception {
        mockMvc.perform(get("/api/v1/transfers/{transferId}", "NONEXISTENT00000000000000")
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("should get transfers by account successfully")
    void shouldGetTransfersByAccountSuccessfully() throws Exception {
        mockMvc.perform(post("/api/v1/transfers")
                .header("Authorization", bearerToken(superAdminToken))
                .with(r -> { r.setRemoteAddr(uniqueIp()); return r; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRequest("REF-CTRL-007"))));

        mockMvc.perform(get("/api/v1/transfers/account/{accountId}", "01JACCTS0000000000000001AA")
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").isNotEmpty());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/transfers")
                        .with(r -> { r.setRemoteAddr(uniqueIp()); return r; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("REF-CTRL-008"))))
                .andExpect(status().isUnauthorized());
    }
}