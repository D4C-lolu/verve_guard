package interswitch.academy.verve_guard.controllers.v1;


import interswitch.academy.verve_guard.base.BaseControllerIntegrationTest;

import interswitch.academy.verve_guard.models.enums.FraudStatus;
import interswitch.academy.verve_guard.models.request.TransactionIngestionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@DisplayName("Fraud Controller Integration Tests")
public class FraudControllerIntegrationTest extends BaseControllerIntegrationTest {

    private String superAdminToken;
    private String merchantToken;

    @BeforeEach
    void setup() throws Exception {
        superAdminToken = loginAndGetAccessToken("superadmin@verveguard.com", "Admin123!");
        merchantToken   = loginAndGetAccessToken("demo.merchant@verveguard.com", "Admin123!");
    }

    @Test
    @DisplayName("should evaluate transaction successfully")
    void shouldEvaluateTransactionSuccessfully() throws Exception {
        TransactionIngestionRequest request = new TransactionIngestionRequest(
                "4111111111111111",
                "01JMERCH0000000000000001AA",
                new BigDecimal("5000.50"),
                "NGN"
        );

        mockMvc.perform(post("/api/v1/fraud/evaluate")
                        .header("Authorization", bearerToken(superAdminToken))
                        .with(r -> { r.setRemoteAddr(uniqueIp()); return r; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(FraudStatus.CLEAN.name()));
    }

    @Test
    @DisplayName("should return BLOCKED for blacklisted merchant")
    void shouldReturnBlockedForBlacklistedMerchant() throws Exception {
        TransactionIngestionRequest request = new TransactionIngestionRequest(
                "4111111111111111",
                "01JMERCH0000000000000002BB",
                new BigDecimal("5000.50"),
                "NGN"
        );

        mockMvc.perform(post("/api/v1/fraud/evaluate")
                        .header("Authorization", bearerToken(superAdminToken))
                        .with(r -> { r.setRemoteAddr(uniqueIp()); return r; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(FraudStatus.BLOCKED.name()));
    }

    @Test
    @DisplayName("should get fraud attempts successfully as super admin")
    void shouldGetFraudAttemptsSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/fraud/attempts")
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("should return 403 when merchant tries to get fraud attempts")
    void shouldReturn403WhenMerchantTriesToGetFraudAttempts() throws Exception {
        mockMvc.perform(get("/api/v1/fraud/attempts")
                        .header("Authorization", bearerToken(merchantToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/fraud/attempts"))
                .andExpect(status().isUnauthorized());
    }
}