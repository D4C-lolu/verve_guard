package interswitch.academy.verve_guard.services;

import com.github.f4b6a3.ulid.UlidCreator;
import interswitch.academy.verve_guard.models.enums.FraudStatus;
import interswitch.academy.verve_guard.models.request.TransactionIngestionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudAttemptLoggerService {

    private final NamedParameterJdbcTemplate namedJdbc;

    private static final String INSERT_FRAUD_ATTEMPT = """
            INSERT INTO fraud_attempts (id, card_hash, merchant_id, ip_address, amount, currency, status, flags, created_at)
            VALUES (:id, :cardHash, :merchantId, :ipAddress, :amount, :currency, :status, :flags, now())
            """;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFraudAttempt(String cardHash, TransactionIngestionRequest request,
                    String ipAddress, FraudStatus status, List<String> flags) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("id", UlidCreator.getUlid().toString())
                    .addValue("cardHash", cardHash)
                    .addValue("merchantId", request.merchantId())
                    .addValue("ipAddress", ipAddress)
                    .addValue("amount", request.amount())
                    .addValue("currency", request.currency())
                    .addValue("status", status.name())
                    .addValue("flags", flags.toArray(new String[0]));

            namedJdbc.update(INSERT_FRAUD_ATTEMPT, params);
        } catch (Exception e) {
            log.error("Failed to log fraud attempt", e);
        }
    }
}
