package interswitch.academy.verve_guard.components;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class BlacklistCache {

    private final NamedParameterJdbcTemplate namedJdbc;

    private static final String IS_BLACKLISTED = """
            SELECT EXISTS(
                SELECT 1 FROM merchant_blacklist
                WHERE merchant_id = :merchantId AND lifted_at IS NULL
            )
            """;

    private final Cache<String, Boolean> cache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats()
            .build();

    public boolean isBlacklisted(String merchantId) {
        Boolean cached = cache.getIfPresent(merchantId);
        if (cached != null) return cached;

        Boolean result = namedJdbc.queryForObject(
                IS_BLACKLISTED,
                new MapSqlParameterSource("merchantId", merchantId),
                Boolean.class
        );

        boolean blacklisted = Boolean.TRUE.equals(result);
        cache.put(merchantId, blacklisted);
        return blacklisted;
    }

    public void invalidate(String merchantId) {
        cache.invalidate(merchantId);
    }

    public CacheStats stats() {
        return cache.stats();
    }
}