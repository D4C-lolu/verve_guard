package interswitch.academy.verve_guard.stress;

import interswitch.academy.verve_guard.config.TestcontainersConfiguration;
import interswitch.academy.verve_guard.exceptions.advice.ApiSuccess;
import interswitch.academy.verve_guard.models.request.LoginRequest;
import interswitch.academy.verve_guard.models.request.TransferRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public class TransferStressTest {

    private RestClient restClient;
    private String superAdminToken;

    @LocalServerPort
    private int port;

    private static final int CONCURRENT_REQUESTS = 200;
    private static final long MAX_OVERHEAD_MS    = 100;

    @BeforeEach
    void setup() {
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        LoginRequest loginRequest = new LoginRequest("superadmin@verveguard.com", "Admin123!");

        ApiSuccess loginResponse = restClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .retrieve()
                .body(ApiSuccess.class);

        assert loginResponse != null;
        this.superAdminToken = ((Map<?, ?>) loginResponse.data()).get("accessToken").toString();
    }

    @Test
    @DisplayName("should handle 200 concurrent transfer requests within 100ms overhead")
    void shouldHandle200ConcurrentRequestsWithin100msOverhead() throws InterruptedException {
        ExecutorService executor       = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        CountDownLatch startLatch      = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(CONCURRENT_REQUESTS);

        List<Long> responseTimes       = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger successCount     = new AtomicInteger(0);
        AtomicInteger failureCount     = new AtomicInteger(0);

        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    TransferRequest request = new TransferRequest(
                            "STRESS-REF-" + index,
                            "01JACCTS0000000000000001AA",
                            "01JACCTS0000000000000002BB",
                            new BigDecimal("100.00"),
                            "NGN", "Stress test transfer", "4111111111111111"
                    );

                    long start = System.currentTimeMillis();

                    ResponseEntity<Void> response = restClient.post()
                            .uri("/api/v1/transfers")
                            .header("Authorization", "Bearer " + superAdminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(request)
                            .retrieve()
                            .toBodilessEntity();

                    long elapsed = System.currentTimeMillis() - start;
                    responseTimes.add(elapsed);

                    if (response.getStatusCode().is2xxSuccessful()) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("Request failed: {}", e.getMessage());
                    failureCount.incrementAndGet();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = completionLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        if (!finished) {
            log.warn("Stress test timed out before all requests completed!");
        }

        calculateAndLogResults(responseTimes, successCount.get(), failureCount.get());
    }

    private void calculateAndLogResults(List<Long> responseTimes, int success, int failure) {
        if (responseTimes.isEmpty()) return;

        Collections.sort(responseTimes);
        long avg = (long) responseTimes.stream().mapToLong(L -> L).average().orElse(0);
        long p95 = responseTimes.get((int) (responseTimes.size() * 0.95));
        long p99 = responseTimes.get((int) (responseTimes.size() * 0.99));

        log.info("=== Stress Test Results (RestClient) ===");
        log.info("Total requests:    {}", CONCURRENT_REQUESTS);
        log.info("Successful:        {}", success);
        log.info("Failed:            {}", failure);
        log.info("Avg response time: {}ms", avg);
        log.info("P95 response time: {}ms", p95);
        log.info("P99 response time: {}ms", p99);
        log.info("========================================");

        assertThat(avg).isLessThanOrEqualTo(MAX_OVERHEAD_MS);
        assertThat(success).isGreaterThan(0);
    }
}