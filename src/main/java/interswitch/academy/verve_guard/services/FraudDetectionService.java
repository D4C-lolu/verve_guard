package interswitch.academy.verve_guard.services;

import interswitch.academy.verve_guard.components.BlacklistCache;
import interswitch.academy.verve_guard.models.response.FraudAttemptResponse;
import interswitch.academy.verve_guard.models.enums.FraudStatus;
import interswitch.academy.verve_guard.models.request.TransactionIngestionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Array;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final NamedParameterJdbcTemplate namedJdbc;
    private final RateLimiterService rateLimiter;
    private final BlacklistCache blacklistCache;
    private final FraudAttemptLoggerService fraudAttemptLoggerService;

    private static final String SELECT_ALL_FRAUD_ATTEMPTS = """
            SELECT * FROM fraud_attempts
            ORDER BY created_at DESC
            LIMIT :limit OFFSET :offset
            """;

    private static final String COUNT_ALL_FRAUD_ATTEMPTS = """
            SELECT COUNT(*) FROM fraud_attempts
            """;

    private static final String CARD_VELOCITY_COUNT = """
            SELECT COUNT(*) FROM fraud_attempts
            WHERE card_hash = :cardHash
            AND created_at >= :since
            """;

    private static final String MERCHANT_SINGLE_LIMIT = """
            SELECT tc.single_transaction_limit
            FROM tier_config tc
            JOIN merchants m ON m.tier = tc.tier
            WHERE m.id = :merchantId AND m.deleted_at IS NULL
            """;

    public FraudStatus evaluate(TransactionIngestionRequest request, String ipAddress) {
        String cardHash = DigestUtils.sha256Hex(request.cardNumber());
        List<String> flags = new ArrayList<>();
        FraudStatus status = FraudStatus.CLEAN;

        if (blacklistCache.isBlacklisted(request.merchantId())) {
            flags.add("MERCHANT_BLACKLISTED");
            fraudAttemptLoggerService.logFraudAttempt(cardHash, request, ipAddress, FraudStatus.BLOCKED, flags);
            return FraudStatus.BLOCKED;
        }

        if (rateLimiter.isRateLimited(ipAddress)) {
            flags.add("RATE_LIMITED");
            fraudAttemptLoggerService.logFraudAttempt(cardHash, request, ipAddress, FraudStatus.BLOCKED, flags);
            return FraudStatus.BLOCKED;
        }

        if (isCardVelocityExceeded(cardHash)) {
            flags.add("CARD_VELOCITY_EXCEEDED");
            status = FraudStatus.SUSPICIOUS;
        }

        if (isAmountExceedsSingleLimit(request.merchantId(), request.amount())) {
            flags.add("EXCEEDS_SINGLE_LIMIT");
            status = FraudStatus.SUSPICIOUS;
        }

        if (isRoundAmount(request.amount())) {
            flags.add("ROUND_AMOUNT");
            status = FraudStatus.SUSPICIOUS;
        }

        fraudAttemptLoggerService.logFraudAttempt(cardHash, request, ipAddress, status, flags);
        return status;
    }

    public Page<FraudAttemptResponse> getFraudAttempts(int page, int size) {
        int offset = (page - 1) * size;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", size)
                .addValue("offset", offset);

        List<FraudAttemptResponse> attempts = namedJdbc.query(
                SELECT_ALL_FRAUD_ATTEMPTS, params, fraudAttemptRowMapper()
        );

        Integer total = namedJdbc.queryForObject(
                COUNT_ALL_FRAUD_ATTEMPTS, new MapSqlParameterSource(), Integer.class
        );

        return new PageImpl<>(attempts, PageRequest.of(page - 1, size), total != null ? total : 0);
    }

    private boolean isCardVelocityExceeded(String cardHash) {
        OffsetDateTime since = OffsetDateTime.now().minusSeconds(60);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("cardHash", cardHash)
                .addValue("since", since);
        Integer count = namedJdbc.queryForObject(CARD_VELOCITY_COUNT, params, Integer.class);
        return count != null && count >= 3;
    }

    private boolean isAmountExceedsSingleLimit(String merchantId, BigDecimal amount) {
        try {
            BigDecimal limit = namedJdbc.queryForObject(
                    MERCHANT_SINGLE_LIMIT,
                    new MapSqlParameterSource("merchantId", merchantId),
                    BigDecimal.class
            );
            return limit != null && amount.compareTo(limit) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isRoundAmount(BigDecimal amount) {
        return amount.remainder(new BigDecimal("1000")).compareTo(BigDecimal.ZERO) == 0;
    }


    private RowMapper<FraudAttemptResponse> fraudAttemptRowMapper() {
        return (rs, rowNum) -> {
            Array flagsArray = rs.getArray("flags");
            List<String> flags = flagsArray != null
                    ? Arrays.asList((String[]) flagsArray.getArray())
                    : List.of();

            return new FraudAttemptResponse(
                    rs.getString("id"),
                    rs.getString("card_hash"),
                    rs.getString("merchant_id"),
                    rs.getString("ip_address"),
                    rs.getBigDecimal("amount"),
                    rs.getString("currency"),
                    FraudStatus.valueOf(rs.getString("status")),
                    flags,
                    rs.getObject("created_at", OffsetDateTime.class)
            );
        };
    }
}