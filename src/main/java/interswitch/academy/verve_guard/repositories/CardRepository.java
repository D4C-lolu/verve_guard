package interswitch.academy.verve_guard.repositories;

import interswitch.academy.verve_guard.entities.Card;
import interswitch.academy.verve_guard.models.enums.CardStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, String> {

    Optional<Card> findByCardNumber(String cardNumber);

    List<Card> findAllByAccountId(String accountId);

    List<Card> findAllByAccountIdAndDeletedAtIsNull(String accountId);

    List<Card> findAllByCardStatus(CardStatus cardStatus);

    boolean existsByCardNumber(String cardNumber);

    int countByAccountIdAndDeletedAtIsNull(String accountId);
}
