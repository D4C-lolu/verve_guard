package interswitch.academy.verve_guard.repositories;

import interswitch.academy.verve_guard.entities.Account;
import interswitch.academy.verve_guard.models.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findAllByMerchantId(String merchantId);

    List<Account> findAllByMerchantIdAndDeletedAtIsNull(String merchantId);

    List<Account> findAllByAccountStatus(AccountStatus accountStatus);

    boolean existsByAccountNumber(String accountNumber);

    int countByMerchantIdAndDeletedAtIsNull(String merchantId);
}
