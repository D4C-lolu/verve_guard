package interswitch.academy.verve_guard.components;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransferRecoveryJob {

    private final NamedParameterJdbcTemplate namedJdbc;

    private static final String SELECT_STUCK_TRANSFERS = """
            SELECT id, from_account_id, to_account_id, amount
            FROM transfers
            WHERE transfer_status = 'PENDING'
            AND created_at < now() - INTERVAL '5 minutes'
            """;

    private static final String MARK_TRANSFER_FAILED = """
            UPDATE transfers
            SET transfer_status = 'FAILED', updated_at = now()
            WHERE id = :id
            """;

    private static final String RECALCULATE_BALANCE = """
            UPDATE accounts a
            SET balance = (
                SELECT COALESCE(SUM(CASE
                    WHEN t.transaction_type = 'CREDIT' THEN t.amount
                    WHEN t.transaction_type = 'DEBIT'  THEN -t.amount
                    ELSE 0
                END), 0)
                FROM transactions t
                WHERE t.account_id = a.id
                AND t.transaction_status = 'SUCCESS'
            ),
            ledger_balance = (
                SELECT COALESCE(SUM(CASE
                    WHEN t.transaction_type = 'CREDIT' THEN t.amount
                    WHEN t.transaction_type = 'DEBIT'  THEN -t.amount
                    ELSE 0
                END), 0)
                FROM transactions t
                WHERE t.account_id = a.id
                AND t.transaction_status IN ('SUCCESS', 'PENDING')
            ),
            updated_at = now()
            WHERE a.deleted_at IS NULL
            """;

    private static final String SELECT_BALANCE_MISMATCHES = """
            SELECT a.id, a.balance AS stored_balance,
                COALESCE(SUM(CASE
                    WHEN t.transaction_type = 'CREDIT' THEN t.amount
                    WHEN t.transaction_type = 'DEBIT'  THEN -t.amount
                    ELSE 0
                END), 0) AS calculated_balance
            FROM accounts a
            LEFT JOIN transactions t ON t.account_id = a.id
                AND t.transaction_status = 'SUCCESS'
            WHERE a.deleted_at IS NULL
            GROUP BY a.id, a.balance
            HAVING a.balance != COALESCE(SUM(CASE
                WHEN t.transaction_type = 'CREDIT' THEN t.amount
                WHEN t.transaction_type = 'DEBIT'  THEN -t.amount
                ELSE 0
            END), 0)
            """;

    private static final String COUNT_TRANSACTIONS_FOR_TRANSFER = """
        SELECT COUNT(*) FROM transactions WHERE transfer_id = :transferId
        """;

    private static final String MARK_TRANSFER_SUCCESS = """
        UPDATE transfers SET transfer_status = 'SUCCESS', updated_at = now()
        WHERE id = :id
        """;


    @Scheduled(cron = "0 0 0,12 * * *")
    @Transactional
    public void recoverStuckTransfers() {
        log.info("Starting stuck transfer recovery job");

        List<Map<String, Object>> stuckTransfers = namedJdbc.queryForList(
                SELECT_STUCK_TRANSFERS, new MapSqlParameterSource()
        );

        if (stuckTransfers.isEmpty()) {
            log.info("No stuck transfers found");
            return;
        }

        log.warn("Found {} stuck transfers", stuckTransfers.size());

        stuckTransfers.forEach(transfer -> {
            String id = (String) transfer.get("id");

            Integer txnCount = namedJdbc.queryForObject(
                    COUNT_TRANSACTIONS_FOR_TRANSFER,
                    new MapSqlParameterSource("transferId", id),
                    Integer.class
            );

            if (txnCount != null && txnCount == 2) {
                namedJdbc.update(MARK_TRANSFER_SUCCESS, new MapSqlParameterSource("id", id));
                log.warn("Transfer {} had transactions — marked SUCCESS", id);
            } else {
                namedJdbc.update(MARK_TRANSFER_FAILED, new MapSqlParameterSource("id", id));
                log.warn("Transfer {} had no transactions — marked FAILED", id);
            }
        });

        log.info("Stuck transfer recovery complete — {} transfers resolved", stuckTransfers.size());
    }

    @Scheduled(cron = "0 30 0,12 * * *")
    @Transactional
    public void reconcileBalances() {
        log.info("Starting balance reconciliation job");

        List<Map<String, Object>> mismatches = namedJdbc.queryForList(
                SELECT_BALANCE_MISMATCHES, new MapSqlParameterSource()
        );

        if (mismatches.isEmpty()) {
            log.info("All account balances are correct");
            return;
        }

        log.warn("Found {} accounts with balance mismatches — reconciling", mismatches.size());
        mismatches.forEach(mismatch ->
                log.warn("Account {} — stored: {}, calculated: {}",
                        mismatch.get("id"),
                        mismatch.get("stored_balance"),
                        mismatch.get("calculated_balance"))
        );

        namedJdbc.update(RECALCULATE_BALANCE, new MapSqlParameterSource());

        log.info("Balance reconciliation complete — {} accounts corrected", mismatches.size());
    }
}