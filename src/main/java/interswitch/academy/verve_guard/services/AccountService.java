package interswitch.academy.verve_guard.services;

import com.github.f4b6a3.ulid.UlidCreator;
import interswitch.academy.verve_guard.exceptions.BadRequestException;
import interswitch.academy.verve_guard.exceptions.NotFoundException;
import interswitch.academy.verve_guard.models.enums.AccountStatus;
import interswitch.academy.verve_guard.models.enums.AccountType;
import interswitch.academy.verve_guard.models.enums.KycStatus;
import interswitch.academy.verve_guard.models.request.AccountResponse;
import interswitch.academy.verve_guard.models.request.CreateAccountRequest;
import interswitch.academy.verve_guard.models.request.CreateMyAccountRequest;
import interswitch.academy.verve_guard.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbc;

    private static final String INSERT_ACCOUNT = """
            INSERT INTO accounts (id, merchant_id, account_number, account_type, currency,
                balance, ledger_balance, account_status, created_at, updated_at, created_by)
            VALUES (:id, :merchantId, :accountNumber, :accountType, :currency,
                0, 0, :accountStatus, now(), now(), :createdBy)
            """;

    private static final String SELECT_BY_ID = """
            SELECT * FROM accounts WHERE id = :id AND deleted_at IS NULL
            """;

    private static final String SELECT_BY_MERCHANT = """
            SELECT * FROM accounts WHERE merchant_id = :merchantId AND deleted_at IS NULL
            ORDER BY %s %s
            LIMIT :limit OFFSET :offset
            """;

    private static final String COUNT_BY_MERCHANT = """
            SELECT COUNT(*) FROM accounts WHERE merchant_id = :merchantId AND deleted_at IS NULL
            """;

    private static final String UPDATE_STATUS = """
            UPDATE accounts SET account_status = :status, updated_at = now(), updated_by = :updatedBy
            WHERE id = :id AND deleted_at IS NULL
            """;

    private static final String SOFT_DELETE = """
            UPDATE accounts SET deleted_at = now(), deleted_by = :deletedBy, updated_at = now()
            WHERE id = :id AND deleted_at IS NULL
            """;

    private static final String EXISTS_MERCHANT = """
            SELECT EXISTS(SELECT 1 FROM merchants WHERE id = :merchantId AND deleted_at IS NULL)
            """;

    private static final String MERCHANT_KYC_STATUS = """
            SELECT kyc_status FROM merchants WHERE id = :merchantId AND deleted_at IS NULL
            """;

    private static final String MERCHANT_TIER = """
            SELECT tier FROM merchants WHERE id = :merchantId AND deleted_at IS NULL
            """;

    private static final String TIER_MAX_ACCOUNTS = """
            SELECT max_accounts FROM tier_config WHERE tier = :tier
            """;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "merchant_id", "account_number", "account_type",
            "currency", "balance", "ledger_balance", "account_status",
            "created_at", "updated_at"
    );

    private static final String MERCHANT_ID_BY_USER = """
        SELECT id FROM merchants WHERE user_id = :userId AND deleted_at IS NULL
        """;

    @Transactional
    public AccountResponse createAccountForSelf(CreateMyAccountRequest request) {
        String userId = SecurityUtil.getCurrentUserId();

        String merchantId = namedJdbc.query(
                MERCHANT_ID_BY_USER,
                new MapSqlParameterSource("userId", userId),
                (rs, rowNum) -> rs.getString("id")
        ).stream().findFirst().orElseThrow(() -> new NotFoundException("Merchant account not found for current user"));

        return createAccount(new CreateAccountRequest(merchantId, request.accountType(), request.currency()));
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        MapSqlParameterSource merchantParams = new MapSqlParameterSource("merchantId", request.merchantId());

        Boolean merchantExists = namedJdbc.queryForObject(EXISTS_MERCHANT, merchantParams, Boolean.class);
        if (!Boolean.TRUE.equals(merchantExists)) {
            throw new NotFoundException("Merchant not found");
        }

        String kycStatus = namedJdbc.queryForObject(MERCHANT_KYC_STATUS, merchantParams, String.class);
        if (!KycStatus.APPROVED.name().equals(kycStatus)) {
            throw new BadRequestException("Merchant must be KYC approved to create an account");
        }

        String tier = namedJdbc.queryForObject(MERCHANT_TIER, merchantParams, String.class);
        Integer maxAccounts = namedJdbc.queryForObject(
                TIER_MAX_ACCOUNTS,
                new MapSqlParameterSource("tier", tier),
                Integer.class
        );

        int currentCount = countAccountsByMerchant(request.merchantId());
        if (maxAccounts != null && currentCount > maxAccounts) {
            throw new BadRequestException("Merchant has reached maximum number of accounts for their tier");
        }

        String accountNumber = jdbcTemplate.queryForObject(
                "SELECT nextval('account_number_seq')", String.class
        );

        String id = UlidCreator.getUlid().toString();
        String createdBy = SecurityUtil.findCurrentUserId().orElse(null);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("merchantId", request.merchantId())
                .addValue("accountNumber", accountNumber)
                .addValue("accountType", request.accountType().name())
                .addValue("currency", request.currency())
                .addValue("accountStatus", AccountStatus.ACTIVE.name())
                .addValue("createdBy", createdBy);

        namedJdbc.update(INSERT_ACCOUNT, params);
        return getAccountById(id);
    }

    public AccountResponse getAccountById(String accountId) {
        MapSqlParameterSource params = new MapSqlParameterSource("id", accountId);
        return namedJdbc.query(SELECT_BY_ID, params, accountRowMapper())
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Account not found"));
    }

    public Page<AccountResponse> getAccountsByMerchant(String merchantId, int page, int size, String sortField, Sort.Direction direction) {
        String safeSortField = validateSortField(sortField);
        int offset = (page - 1) * size;
        String query = SELECT_BY_MERCHANT.formatted(safeSortField, direction.name());

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("merchantId", merchantId)
                .addValue("limit", size)
                .addValue("offset", offset);

        List<AccountResponse> accounts = namedJdbc.query(query, params, accountRowMapper());
        int total = countAccountsByMerchant(merchantId);
        return new PageImpl<>(accounts, PageRequest.of(page - 1, size), total);
    }

    @Transactional
    public AccountResponse updateAccountStatus(String accountId, AccountStatus status) {
        getAccountById(accountId);

        String updatedBy = SecurityUtil.findCurrentUserId().orElse(null);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", accountId)
                .addValue("status", status.name())
                .addValue("updatedBy", updatedBy);

        namedJdbc.update(UPDATE_STATUS, params);
        return getAccountById(accountId);
    }

    @Transactional
    public void deleteAccount(String accountId) {
        getAccountById(accountId);

        String deletedBy = SecurityUtil.getCurrentUserId();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", accountId)
                .addValue("deletedBy", deletedBy);

        namedJdbc.update(SOFT_DELETE, params);
    }

    private int countAccountsByMerchant(String merchantId) {
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

    private RowMapper<AccountResponse> accountRowMapper() {
        return (rs, rowNum) -> new AccountResponse(
                rs.getString("id"),
                rs.getString("merchant_id"),
                rs.getString("account_number"),
                AccountType.valueOf(rs.getString("account_type")),
                rs.getString("currency"),
                rs.getBigDecimal("balance"),
                rs.getBigDecimal("ledger_balance"),
                AccountStatus.valueOf(rs.getString("account_status")),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class)
        );
    }
}