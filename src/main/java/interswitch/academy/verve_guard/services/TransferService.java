package interswitch.academy.verve_guard.services;

import com.github.f4b6a3.ulid.UlidCreator;
import interswitch.academy.verve_guard.exceptions.BadRequestException;
import interswitch.academy.verve_guard.exceptions.ConflictException;
import interswitch.academy.verve_guard.exceptions.ForbiddenException;
import interswitch.academy.verve_guard.exceptions.NotFoundException;
import interswitch.academy.verve_guard.models.enums.FraudStatus;
import interswitch.academy.verve_guard.models.enums.TransactionType;
import interswitch.academy.verve_guard.models.enums.TransferStatus;
import interswitch.academy.verve_guard.models.request.TransactionIngestionRequest;
import interswitch.academy.verve_guard.models.request.TransferRequest;
import interswitch.academy.verve_guard.models.response.TransferResponse;
import interswitch.academy.verve_guard.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final NamedParameterJdbcTemplate namedJdbc;
    private final FraudDetectionService fraudDetectionService;

    private static final String EXISTS_REFERENCE = """
            SELECT EXISTS(SELECT 1 FROM transfers WHERE reference = :reference)
            """;

    private static final String SELECT_ACCOUNT = """
            SELECT * FROM accounts WHERE id = :id AND deleted_at IS NULL
            """;

    private static final String INSERT_TRANSFER = """
            INSERT INTO transfers (id, reference, from_account_id, to_account_id, amount,
                currency, transfer_status, description, created_at, updated_at, created_by)
            VALUES (:id, :reference, :fromAccountId, :toAccountId, :amount,
                :currency, :status, :description, now(), now(), :createdBy)
            """;

    private static final String INSERT_TRANSACTION = """
            INSERT INTO transactions (id, account_id, card_id, transfer_id, transaction_type,
                channel, amount, fee, currency, transaction_status, created_at, updated_at, created_by)
            VALUES (:id, :accountId, :cardId, :transferId, :type,
                'TRANSFER', :amount, 0, :currency, 'SUCCESS', now(), now(), :createdBy)
            """;

    private static final String DEBIT_ACCOUNT = """
            UPDATE accounts
            SET balance = balance - :amount,
                ledger_balance = ledger_balance - :amount,
                updated_at = now()
            WHERE id = :id AND deleted_at IS NULL
            """;

    private static final String CREDIT_ACCOUNT = """
            UPDATE accounts
            SET balance = balance + :amount,
                ledger_balance = ledger_balance + :amount,
                updated_at = now()
            WHERE id = :id AND deleted_at IS NULL
            """;

    private static final String UPDATE_TRANSFER_STATUS = """
            UPDATE transfers SET transfer_status = :status, updated_at = now()
            WHERE id = :id
            """;

    private static final String SELECT_TRANSFER_BY_ID = """
            SELECT * FROM transfers WHERE id = :id
            """;

    private static final String SELECT_TRANSFERS_BY_ACCOUNT = """
            SELECT t.* FROM transfers t
            WHERE (t.from_account_id = :accountId OR t.to_account_id = :accountId)
            ORDER BY t.created_at DESC
            LIMIT :limit OFFSET :offset
            """;

    private static final String COUNT_TRANSFERS_BY_ACCOUNT = """
            SELECT COUNT(*) FROM transfers
            WHERE from_account_id = :accountId OR to_account_id = :accountId
            """;

    private static final String DAILY_DEBIT_SUM = """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM transactions t
            WHERE t.account_id = :accountId
            AND t.transaction_type = 'DEBIT'
            AND t.transaction_status = 'SUCCESS'
            AND t.created_at >= :from
            AND t.created_at <= :to
            """;

    private static final String MONTHLY_DEBIT_SUM = """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM transactions t
            WHERE t.account_id = :accountId
            AND t.transaction_type = 'DEBIT'
            AND t.transaction_status = 'SUCCESS'
            AND t.created_at >= :from
            AND t.created_at <= :to
            """;

    private static final String TIER_CONFIG_BY_ACCOUNT = """
            SELECT tc.* FROM tier_config tc
            JOIN merchants m ON m.tier = tc.tier
            JOIN accounts a ON a.merchant_id = m.id
            WHERE a.id = :accountId AND a.deleted_at IS NULL
            """;

    private static final String MERCHANT_OWNS_ACCOUNT = """
            SELECT EXISTS(
                SELECT 1 FROM accounts a
                JOIN merchants m ON a.merchant_id = m.id
                WHERE a.id = :accountId AND m.user_id = :userId AND a.deleted_at IS NULL
            )
            """;

    @Transactional
    public TransferResponse transfer(TransferRequest request, String ipAddress) {
        FraudStatus fraudStatus = fraudDetectionService.evaluate(
                new TransactionIngestionRequest(
                        request.cardNumber(),
                        getAccountMerchantId(request.fromAccountId()),
                        request.amount(),
                        request.currency()
                ),
                ipAddress
        );

        if (fraudStatus == FraudStatus.BLOCKED) {
            throw new BadRequestException("Transaction blocked due to fraud detection");
        }

        Boolean referenceExists = namedJdbc.queryForObject(
                EXISTS_REFERENCE,
                new MapSqlParameterSource("reference", request.reference()),
                Boolean.class
        );
        if (Boolean.TRUE.equals(referenceExists)) {
            throw new ConflictException("Transfer reference already exists");
        }

        Map<String, Object> fromAccount = getAccount(request.fromAccountId());
        Map<String, Object> toAccount   = getAccount(request.toAccountId());

        if (!fromAccount.get("currency").equals(request.currency()) ||
            !toAccount.get("currency").equals(request.currency())) {
            throw new BadRequestException("Account currency does not match transfer currency");
        }

        BigDecimal balance = (BigDecimal) fromAccount.get("balance");
        if (balance.compareTo(request.amount()) < 0) {
            throw new BadRequestException("Insufficient funds");
        }

        validateTierLimits(request.fromAccountId(), request.amount());

        String transferId = UlidCreator.getUlid().toString();
        String createdBy  = SecurityUtil.findCurrentUserId().orElse(null);

        namedJdbc.update(INSERT_TRANSFER, new MapSqlParameterSource()
                .addValue("id", transferId)
                .addValue("reference", request.reference())
                .addValue("fromAccountId", request.fromAccountId())
                .addValue("toAccountId", request.toAccountId())
                .addValue("amount", request.amount())
                .addValue("currency", request.currency())
                .addValue("status", TransferStatus.PENDING.name())
                .addValue("description", request.description())
                .addValue("createdBy", createdBy));

        String debitTxnId  = UlidCreator.getUlid().toString();
        String creditTxnId = UlidCreator.getUlid().toString();

        namedJdbc.update(INSERT_TRANSACTION, new MapSqlParameterSource()
                .addValue("id", debitTxnId)
                .addValue("accountId", request.fromAccountId())
                .addValue("cardId", null)
                .addValue("transferId", transferId)
                .addValue("type", TransactionType.DEBIT.name())
                .addValue("amount", request.amount())
                .addValue("currency", request.currency())
                .addValue("createdBy", createdBy));

        namedJdbc.update(INSERT_TRANSACTION, new MapSqlParameterSource()
                .addValue("id", creditTxnId)
                .addValue("accountId", request.toAccountId())
                .addValue("cardId", null)
                .addValue("transferId", transferId)
                .addValue("type", TransactionType.CREDIT.name())
                .addValue("amount", request.amount())
                .addValue("currency", request.currency())
                .addValue("createdBy", createdBy));

        namedJdbc.update(DEBIT_ACCOUNT, new MapSqlParameterSource()
                .addValue("id", request.fromAccountId())
                .addValue("amount", request.amount()));

        namedJdbc.update(CREDIT_ACCOUNT, new MapSqlParameterSource()
                .addValue("id", request.toAccountId())
                .addValue("amount", request.amount()));

        namedJdbc.update(UPDATE_TRANSFER_STATUS, new MapSqlParameterSource()
                .addValue("id", transferId)
                .addValue("status", TransferStatus.SUCCESS.name()));

        return getTransferById(transferId);
    }

    @Transactional
    public TransferResponse transferForSelf(TransferRequest request, String ipAddress) {
        String userId = SecurityUtil.getCurrentUserId();

        Boolean ownsAccount = namedJdbc.queryForObject(
                MERCHANT_OWNS_ACCOUNT,
                new MapSqlParameterSource()
                        .addValue("accountId", request.fromAccountId())
                        .addValue("userId", userId),
                Boolean.class
        );

        if (!Boolean.TRUE.equals(ownsAccount)) {
            throw new ForbiddenException("Account does not belong to your merchant");
        }

        return transfer(request, ipAddress);
    }

    public TransferResponse getTransferById(String transferId) {
        return namedJdbc.query(
                SELECT_TRANSFER_BY_ID,
                new MapSqlParameterSource("id", transferId),
                transferRowMapper()
        ).stream().findFirst().orElseThrow(() -> new NotFoundException("Transfer not found"));
    }

    public Page<TransferResponse> getTransfersByAccount(String accountId, int page, int size) {
        int offset = (page - 1) * size;

        List<TransferResponse> transfers = namedJdbc.query(
                SELECT_TRANSFERS_BY_ACCOUNT,
                new MapSqlParameterSource()
                        .addValue("accountId", accountId)
                        .addValue("limit", size)
                        .addValue("offset", offset),
                transferRowMapper()
        );

        Integer total = namedJdbc.queryForObject(
                COUNT_TRANSFERS_BY_ACCOUNT,
                new MapSqlParameterSource("accountId", accountId),
                Integer.class
        );

        return new PageImpl<>(transfers, PageRequest.of(page - 1, size), total != null ? total : 0);
    }

    private void validateTierLimits(String accountId, BigDecimal amount) {
        Map<String, Object> tierConfig = namedJdbc.queryForMap(
                TIER_CONFIG_BY_ACCOUNT,
                new MapSqlParameterSource("accountId", accountId)
        );

        BigDecimal singleLimit   = (BigDecimal) tierConfig.get("single_transaction_limit");
        BigDecimal dailyLimit    = (BigDecimal) tierConfig.get("daily_transaction_limit");
        BigDecimal monthlyLimit  = (BigDecimal) tierConfig.get("monthly_transaction_limit");

        if (amount.compareTo(singleLimit) > 0) {
            throw new BadRequestException("Amount exceeds single transaction limit for your tier");
        }

        OffsetDateTime now          = OffsetDateTime.now();
        OffsetDateTime startOfDay   = now.toLocalDate().atStartOfDay().atOffset(now.getOffset());
        OffsetDateTime startOfMonth = now.toLocalDate().withDayOfMonth(1).atStartOfDay().atOffset(now.getOffset());

        BigDecimal dailyTotal = namedJdbc.queryForObject(
                DAILY_DEBIT_SUM,
                new MapSqlParameterSource()
                        .addValue("accountId", accountId)
                        .addValue("from", startOfDay)
                        .addValue("to", now),
                BigDecimal.class
        );

        if (dailyTotal != null && dailyTotal.add(amount).compareTo(dailyLimit) > 0) {
            throw new BadRequestException("Amount exceeds daily transaction limit for your tier");
        }

        BigDecimal monthlyTotal = namedJdbc.queryForObject(
                MONTHLY_DEBIT_SUM,
                new MapSqlParameterSource()
                        .addValue("accountId", accountId)
                        .addValue("from", startOfMonth)
                        .addValue("to", now),
                BigDecimal.class
        );

        if (monthlyTotal != null && monthlyTotal.add(amount).compareTo(monthlyLimit) > 0) {
            throw new BadRequestException("Amount exceeds monthly transaction limit for your tier");
        }
    }

    private Map<String, Object> getAccount(String accountId) {
        return namedJdbc.query(
                SELECT_ACCOUNT,
                new MapSqlParameterSource("id", accountId),
                (rs, rowNum) -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", rs.getString("id"));
                    map.put("balance", rs.getBigDecimal("balance"));
                    map.put("currency", rs.getString("currency"));
                    map.put("account_status", rs.getString("account_status"));
                    map.put("merchant_id", rs.getString("merchant_id"));
                    return map;
                }
        ).stream().findFirst().orElseThrow(() -> new NotFoundException("Account not found"));
    }

    private String getAccountMerchantId(String accountId) {
        return namedJdbc.queryForObject(
                "SELECT merchant_id FROM accounts WHERE id = :id AND deleted_at IS NULL",
                new MapSqlParameterSource("id", accountId),
                String.class
        );
    }

    private RowMapper<TransferResponse> transferRowMapper() {
        return (rs, rowNum) -> new TransferResponse(
                rs.getString("id"),
                rs.getString("reference"),
                rs.getString("from_account_id"),
                rs.getString("to_account_id"),
                rs.getBigDecimal("amount"),
                rs.getString("currency"),
                TransferStatus.valueOf(rs.getString("transfer_status")),
                rs.getString("description"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class)
        );
    }
}