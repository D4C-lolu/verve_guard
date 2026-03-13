package interswitch.academy.verve_guard.security;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenStore {

    private final Cache<String, String> accessTokenCache;
    private final Cache<String, String> refreshTokenCache;

    public void revokeAccessToken(String userId, String jti) {
        accessTokenCache.put("access:" + userId + ":" + jti, userId);
    }

    public void revokeRefreshToken(String userId, String jti) {
        refreshTokenCache.put("refresh:" + userId + ":" + jti, userId);
    }

    public boolean isAccessTokenRevoked(String userId, String jti) {
        return accessTokenCache.getIfPresent("access:" + userId + ":" + jti) != null;
    }

    public boolean isRefreshTokenRevoked(String userId, String jti) {
        return refreshTokenCache.getIfPresent("refresh:" + userId + ":" + jti) != null;
    }

    public void revokeAllUserTokens(String userId) {
        accessTokenCache.asMap().keySet().removeIf(key -> key.startsWith("access:" + userId + ":"));
        refreshTokenCache.asMap().keySet().removeIf(key -> key.startsWith("refresh:" + userId + ":"));
    }
}