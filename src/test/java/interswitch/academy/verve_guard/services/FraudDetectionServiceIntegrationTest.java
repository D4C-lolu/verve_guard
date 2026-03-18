package interswitch.academy.verve_guard.services;


import interswitch.academy.verve_guard.base.BaseIntegrationTest;
import interswitch.academy.verve_guard.entities.User;
import interswitch.academy.verve_guard.models.enums.*;
import interswitch.academy.verve_guard.models.request.*;
import interswitch.academy.verve_guard.models.response.FraudAttemptResponse;
import interswitch.academy.verve_guard.repositories.UserRepository;
import interswitch.academy.verve_guard.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Fraud Detection Service Integration Tests")
public class FraudDetectionServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private FraudDetectionService fraudDetectionService;

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

    private TransactionIngestionRequest buildRequest(String cardNumber, BigDecimal amount) {
        return new TransactionIngestionRequest(
                cardNumber,
                "01JMERCH0000000000000001AA",
                amount,
                "NGN"
        );
    }

    @Test
    @DisplayName("should return CLEAN for legitimate transaction")
    void shouldReturnCleanForLegitimateTransaction() {
        TransactionIngestionRequest request = buildRequest("4111111111111111", new BigDecimal("5000.50"));

        FraudStatus status = fraudDetectionService.evaluate(request, "192.168.1.1");

        assertThat(status).isEqualTo(FraudStatus.CLEAN);
    }

    @Test
    @DisplayName("should return BLOCKED for blacklisted merchant")
    void shouldReturnBlockedForBlacklistedMerchant() {
        TransactionIngestionRequest request = new TransactionIngestionRequest(
                "4111111111111111",
                "01JMERCH0000000000000002BB",
                new BigDecimal("5000.00"),
                "NGN"
        );

        FraudStatus status = fraudDetectionService.evaluate(request, "192.168.1.2");

        assertThat(status).isEqualTo(FraudStatus.BLOCKED);
    }

    @Test
    @DisplayName("should return BLOCKED when rate limit exceeded")
    void shouldReturnBlockedWhenRateLimitExceeded() {
        TransactionIngestionRequest request = buildRequest("4222222222222222", new BigDecimal("5000.50"));
        String ip = "10.0.0.1";

        for (int i = 0; i <= 5; i++) {
            fraudDetectionService.evaluate(request, ip);
        }

        FraudStatus status = fraudDetectionService.evaluate(request, ip);
        assertThat(status).isEqualTo(FraudStatus.BLOCKED);
    }

    @Test
    @DisplayName("should return SUSPICIOUS for round amount")
    void shouldReturnSuspiciousForRoundAmount() {
        TransactionIngestionRequest request = buildRequest("4333333333333333", new BigDecimal("5000.00"));

        FraudStatus status = fraudDetectionService.evaluate(request, "192.168.1.3");

        assertThat(status).isEqualTo(FraudStatus.SUSPICIOUS);
    }

    @Test
    @DisplayName("should return SUSPICIOUS when card velocity exceeded")
    void shouldReturnSuspiciousWhenCardVelocityExceeded() {
        String cardNumber = "4444444444444444";
        TransactionIngestionRequest request = buildRequest(cardNumber, new BigDecimal("5000.50"));

        for (int i = 0; i < 3; i++) {
            fraudDetectionService.evaluate(request, "192.168.1." + i);
        }

        FraudStatus status = fraudDetectionService.evaluate(request, "192.168.1.99");
        assertThat(status).isEqualTo(FraudStatus.SUSPICIOUS);
    }

    @Test
    @DisplayName("should return SUSPICIOUS when amount exceeds single transaction limit")
    void shouldReturnSuspiciousWhenAmountExceedsSingleLimit() {
        TransactionIngestionRequest request = buildRequest(
                "4555555555555555", new BigDecimal("999999999.00")
        );

        FraudStatus status = fraudDetectionService.evaluate(request, "192.168.1.4");

        assertThat(status).isEqualTo(FraudStatus.SUSPICIOUS);
    }

    @Test
    @DisplayName("should log fraud attempt successfully")
    void shouldLogFraudAttemptSuccessfully() {
        TransactionIngestionRequest request = buildRequest("4666666666666666", new BigDecimal("5000.50"));

        fraudDetectionService.evaluate(request, "192.168.1.5");

        Page<FraudAttemptResponse> attempts = fraudDetectionService.getFraudAttempts(1, 10);
        assertThat(attempts.getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("should get fraud attempts paginated")
    void shouldGetFraudAttemptsPaginated() {
        TransactionIngestionRequest request = buildRequest("4777777777777777", new BigDecimal("5000.50"));
        fraudDetectionService.evaluate(request, "192.168.1.6");

        Page<FraudAttemptResponse> page = fraudDetectionService.getFraudAttempts(1, 10);

        assertThat(page).isNotNull();
        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent().size()).isLessThanOrEqualTo(10);
    }
}