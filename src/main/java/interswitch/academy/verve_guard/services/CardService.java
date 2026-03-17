package interswitch.academy.verve_guard.services;

import com.github.f4b6a3.ulid.UlidCreator;
import interswitch.academy.verve_guard.exceptions.BadRequestException;
import interswitch.academy.verve_guard.exceptions.ConflictException;
import interswitch.academy.verve_guard.exceptions.ForbiddenException;
import interswitch.academy.verve_guard.exceptions.NotFoundException;
import interswitch.academy.verve_guard.models.enums.*;
import interswitch.academy.verve_guard.models.request.CreateCardRequest;
import interswitch.academy.verve_guard.models.request.CreateMyCardRequest;
import interswitch.academy.verve_guard.models.response.CardResponse;
import interswitch.academy.verve_guard.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CardService {

    private final NamedParameterJdbcTemplate namedJdbc;

    private static final String INSERT_CARD = """
        INSERT INTO cards (id, card_number, card_hash, account_id, card_type, scheme,
            expiry_month, expiry_year, card_status, created_at, updated_at, created_by)
        VALUES (:id, :cardNumber, :cardHash, :accountId, :cardType, :scheme,
            :expiryMonth, :expiryYear, :cardStatus, now(), now(), :createdBy)
        """;

    private static final String EXISTS_CARD_HASH = """
        SELECT EXISTS(SELECT 1 FROM cards WHERE card_hash = :cardHash AND deleted_at IS NULL)
        """;

    private static final String SELECT_BY_ID = """
            SELECT * FROM cards WHERE id = :id AND deleted_at IS NULL
            """;

    private static final String SELECT_BY_ACCOUNT = """
            SELECT * FROM cards WHERE account_id = :accountId AND deleted_at IS NULL
            ORDER BY %s %s
            LIMIT :limit OFFSET :offset
            """;

    private static final String COUNT_BY_ACCOUNT = """
            SELECT COUNT(*) FROM cards WHERE account_id = :accountId AND deleted_at IS NULL
            """;

    private static final String COUNT_BY_MERCHANT = """
            SELECT COUNT(*) FROM cards c
            JOIN accounts a ON c.account_id = a.id
            WHERE a.merchant_id = :merchantId AND c.deleted_at IS NULL
            """;

    private static final String UPDATE_STATUS = """
            UPDATE cards SET card_status = :status, updated_at = now(), updated_by = :updatedBy
            WHERE id = :id AND deleted_at IS NULL
            """;

    private static final String SOFT_DELETE = """
            UPDATE cards SET deleted_at = now(), deleted_by = :deletedBy, updated_at = now()
            WHERE id = :id AND deleted_at IS NULL
            """;

    private static final String EXISTS_ACCOUNT = """
            SELECT EXISTS(SELECT 1 FROM accounts WHERE id = :accountId AND deleted_at IS NULL)
            """;

    private static final String MERCHANT_OWNS_CARD = """
        SELECT EXISTS(
            SELECT 1 FROM cards c
            JOIN accounts a ON c.account_id = a.id
            JOIN merchants m ON a.merchant_id = m.id
            WHERE c.id = :cardId AND m.user_id = :userId AND c.deleted_at IS NULL
        )
        """;

    private static final String BLOCK_CARD = """
        UPDATE cards SET card_status = 'BLOCKED', updated_at = now(), updated_by = :updatedBy
        WHERE id = :id AND deleted_at IS NULL AND card_status != 'BLOCKED'
        """;

    private static final String MERCHANT_ID_BY_ACCOUNT = """
            SELECT merchant_id FROM accounts WHERE id = :accountId AND deleted_at IS NULL
            """;

    private static final String MERCHANT_TIER = """
            SELECT tier FROM merchants WHERE id = :merchantId AND deleted_at IS NULL
            """;

    private static final String TIER_MAX_CARDS = """
            SELECT max_cards FROM tier_config WHERE tier = :tier
            """;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "account_id", "card_number", "card_type",
            "scheme", "expiry_month", "expiry_year", "card_status",
            "created_at", "updated_at"
    );

    private static final String MERCHANT_ID_BY_USER = """
        SELECT id FROM merchants WHERE user_id = :userId AND deleted_at IS NULL
        """;

    private static final String EXISTS_ACCOUNT_FOR_MERCHANT = """
        SELECT EXISTS(
            SELECT 1 FROM accounts
            WHERE id = :accountId AND merchant_id = :merchantId AND deleted_at IS NULL
        )
        """;

    @Transactional
    public CardResponse createCardForSelf(CreateMyCardRequest request) {
        String userId = SecurityUtil.getCurrentUserId();

        String merchantId = namedJdbc.query(
                MERCHANT_ID_BY_USER,
                new MapSqlParameterSource("userId", userId),
                (rs, rowNum) -> rs.getString("id")
        ).stream().findFirst().orElseThrow(() -> new NotFoundException("Merchant account not found for current user"));

        Boolean accountBelongsToMerchant = namedJdbc.queryForObject(
                EXISTS_ACCOUNT_FOR_MERCHANT,
                new MapSqlParameterSource()
                        .addValue("accountId", request.accountId())
                        .addValue("merchantId", merchantId),
                Boolean.class
        );

        if (!Boolean.TRUE.equals(accountBelongsToMerchant)) {
            throw new ForbiddenException("Account does not belong to you");
        }

        return createCard(new CreateCardRequest(
                request.accountId(),
                request.cardNumber(),
                request.cardType(),
                request.scheme(),
                request.expiryMonth(),
                request.expiryYear()
        ));
    }

    @Transactional
    public CardResponse createCard(CreateCardRequest request) {
        MapSqlParameterSource accountParams = new MapSqlParameterSource("accountId", request.accountId());

        Boolean accountExists = namedJdbc.queryForObject(EXISTS_ACCOUNT, accountParams, Boolean.class);
        if (!Boolean.TRUE.equals(accountExists)) {
            throw new NotFoundException("Account not found");
        }

        validateCardExpiry(request.expiryMonth(), request.expiryYear());

        String cardHash = DigestUtils.sha256Hex(request.cardNumber());
        Boolean cardExists = namedJdbc.queryForObject(
                EXISTS_CARD_HASH,
                new MapSqlParameterSource("cardHash", cardHash),
                Boolean.class
        );
        if (Boolean.TRUE.equals(cardExists)) {
            throw new ConflictException("Card already exists");
        }

        String merchantId = namedJdbc.queryForObject(MERCHANT_ID_BY_ACCOUNT, accountParams, String.class);
        String tier = namedJdbc.queryForObject(
                MERCHANT_TIER,
                new MapSqlParameterSource("merchantId", merchantId),
                String.class
        );
        Integer maxCards = namedJdbc.queryForObject(
                TIER_MAX_CARDS,
                new MapSqlParameterSource("tier", tier),
                Integer.class
        );

        int currentCardCount = countCardsByMerchant(merchantId);
        if (maxCards != null && currentCardCount >= maxCards) {
            throw new BadRequestException("Merchant has reached maximum number of cards for their tier");
        }

        String maskedCardNumber = maskCardNumber(request.cardNumber());
        String id = UlidCreator.getUlid().toString();
        String createdBy = SecurityUtil.findCurrentUserId().orElse(null);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("cardNumber", maskedCardNumber)
                .addValue("cardHash", cardHash)
                .addValue("accountId", request.accountId())
                .addValue("cardType", request.cardType().name())
                .addValue("scheme", request.scheme().name())
                .addValue("expiryMonth", request.expiryMonth())
                .addValue("expiryYear", request.expiryYear())
                .addValue("cardStatus", CardStatus.ACTIVE.name())
                .addValue("createdBy", createdBy);

        namedJdbc.update(INSERT_CARD, params);
        return getCardById(id);
    }

    public CardResponse getCardById(String cardId) {
        MapSqlParameterSource params = new MapSqlParameterSource("id", cardId);
        return namedJdbc.query(SELECT_BY_ID, params, cardRowMapper())
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Card not found"));
    }

    public void blockCard(String cardId) {
        getCardById(cardId);

        String updatedBy = SecurityUtil.findCurrentUserId().orElse(null);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", cardId)
                .addValue("updatedBy", updatedBy);

        namedJdbc.update(BLOCK_CARD, params);
    }

    public void blockCardForSelf(String cardId) {
        String userId = SecurityUtil.getCurrentUserId();

        Boolean ownsCard = namedJdbc.queryForObject(
                MERCHANT_OWNS_CARD,
                new MapSqlParameterSource()
                        .addValue("cardId", cardId)
                        .addValue("userId", userId),
                Boolean.class
        );

        if (!Boolean.TRUE.equals(ownsCard)) {
            throw new ForbiddenException("Card does not belong to your merchant");
        }

        blockCard(cardId);
    }

    public Page<CardResponse> getCardsByAccount(String accountId, int page, int size, String sortField, Sort.Direction direction) {
        String safeSortField = validateSortField(sortField);
        int offset = (page - 1) * size;
        String query = SELECT_BY_ACCOUNT.formatted(safeSortField, direction.name());

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("accountId", accountId)
                .addValue("limit", size)
                .addValue("offset", offset);

        List<CardResponse> cards = namedJdbc.query(query, params, cardRowMapper());
        int total = countCardsByAccount(accountId);
        return new PageImpl<>(cards, PageRequest.of(page - 1, size), total);
    }

    @Transactional
    public CardResponse updateCardStatus(String cardId, CardStatus status) {
        getCardById(cardId);

        String updatedBy = SecurityUtil.findCurrentUserId().orElse(null);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", cardId)
                .addValue("status", status.name())
                .addValue("updatedBy", updatedBy);

        namedJdbc.update(UPDATE_STATUS, params);
        return getCardById(cardId);
    }

    @Transactional
    public void deleteCard(String cardId) {
        getCardById(cardId);

        String deletedBy = SecurityUtil.getCurrentUserId();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", cardId)
                .addValue("deletedBy", deletedBy);

        namedJdbc.update(SOFT_DELETE, params);
    }

    private void validateCardExpiry(int expiryMonth, int expiryYear) {
        YearMonth expiry = YearMonth.of(expiryYear, expiryMonth);
        if (expiry.isBefore(YearMonth.now())) {
            throw new BadRequestException("Card expiry date is in the past");
        }
    }

    private String maskCardNumber(String cardNumber) {
        return cardNumber.substring(0, 4) +
                "*".repeat(cardNumber.length() - 8) +
                cardNumber.substring(cardNumber.length() - 4);
    }

    private int countCardsByAccount(String accountId) {
        MapSqlParameterSource params = new MapSqlParameterSource("accountId", accountId);
        Integer count = namedJdbc.queryForObject(COUNT_BY_ACCOUNT, params, Integer.class);
        return count != null ? count : 0;
    }

    private int countCardsByMerchant(String merchantId) {
        MapSqlParameterSource params = new MapSqlParameterSource("merchantId", merchantId);
        Integer count = namedJdbc.queryForObject(COUNT_BY_MERCHANT, params, Integer.class);
        return count != null ? count : 0;
    }

    private String validateSortField(String sortField) {
        if (!ALLOWED_SORT_FIELDS.contains(sortField)) {
            throw new BadRequestException("Invalid sort field: " + sortField);
        }
        return sortField;
    }

    private RowMapper<CardResponse> cardRowMapper() {
        return (rs, rowNum) -> new CardResponse(
                rs.getString("id"),
                rs.getString("account_id"),
                rs.getString("card_number"),
                CardType.valueOf(rs.getString("card_type")),
                CardScheme.valueOf(rs.getString("scheme")),
                rs.getInt("expiry_month"),
                rs.getInt("expiry_year"),
                CardStatus.valueOf(rs.getString("card_status")),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class)
        );
    }
}