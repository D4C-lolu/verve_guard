package interswitch.academy.verve_guard.services;

import interswitch.academy.verve_guard.models.request.LoginRequest;
import interswitch.academy.verve_guard.models.response.AuthResponse;
import interswitch.academy.verve_guard.security.UserDetailsServiceImpl;
import interswitch.academy.verve_guard.security.UserPrincipal;
import interswitch.academy.verve_guard.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UserDetailsServiceImpl userDetailsService;

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserByUsername(request.email());
        return tokenService.issueTokens(principal);
    }

    public AuthResponse refresh(String refreshToken) {
        return tokenService.refresh(refreshToken);
    }

    public void logout(String accessHeader, String refreshToken) {

        String accessToken = SecurityUtil.extractToken(accessHeader);
        tokenService.revoke(accessToken, refreshToken);
    }

    public void logoutAll() {
        String userId = SecurityUtil.getCurrentUserId();
        tokenService.revokeAll(userId);
    }
   }