package interswitch.academy.verve_guard.security;

import interswitch.academy.verve_guard.exceptions.InvalidTokenException;
import interswitch.academy.verve_guard.models.response.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenStore tokenStore;
    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    public AuthResponse issueTokens(UserPrincipal principal) {
        String accessToken  = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);
        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse refresh(String refreshToken) {
        if (!jwtService.isTokenValid(refreshToken)) {
            throw new InvalidTokenException("Refresh token is invalid or expired");
        }

        String userId = jwtService.extractUserId(refreshToken);
        String jti    = jwtService.extractJti(refreshToken);

        if (tokenStore.isRefreshTokenRevoked(userId, jti)) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        // rotate — revoke old refresh token
        tokenStore.revokeRefreshToken(userId, jti);

        // load user and issue new pair
        UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserByUsername(userId);
        return issueTokens(principal);
    }

    public void revoke(String accessToken, String refreshToken) {
        String userId    = jwtService.extractUserId(accessToken);
        String accessJti = jwtService.extractJti(accessToken);
        String refreshJti = jwtService.extractJti(refreshToken);
        tokenStore.revokeAccessToken(userId, accessJti);
        tokenStore.revokeRefreshToken(userId, refreshJti);
    }

    public void revokeAll(String userId) {
        tokenStore.revokeAllUserTokens(userId);
    }

    public boolean isAccessTokenValid(String token) {
        if (!jwtService.isTokenValid(token)) return false;
        String userId = jwtService.extractUserId(token);
        String jti    = jwtService.extractJti(token);
        return !tokenStore.isAccessTokenRevoked(userId, jti);
    }
}
