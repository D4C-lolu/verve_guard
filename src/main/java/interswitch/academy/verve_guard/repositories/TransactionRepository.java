package interswitch.academy.verve_guard.repositories;

import interswitch.academy.verve_guard.entities.Transaction;
import interswitch.academy.verve_guard.models.enums.TransactionStatus;
import interswitch.academy.verve_guard.models.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, String> {


    List<Transaction> findAllByAccountId(String accountId);

    List<Transaction> findAllByTransferId(String transferId);

    List<Transaction> findAllByAccountIdAndTransactionStatus(String accountId, TransactionStatus status);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.account.id = :accountId " +
            "AND t.transactionType = :type " +
            "AND t.createdAt >= :from AND t.createdAt <= :to")
    BigDecimal sumAmountByAccountIdAndTypeAndCreatedAtBetween(
            @Param("accountId") String accountId,
            @Param("type") TransactionType type,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );
}
