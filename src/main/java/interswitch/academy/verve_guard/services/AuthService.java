package interswitch.academy.verve_guard.services;

import interswitch.academy.verve_guard.models.request.LoginRequest;
import interswitch.academy.verve_guard.models.response.AuthResponse;
import interswitch.academy.verve_guard.repositories.UserRepository;
import interswitch.academy.verve_guard.security.TokenService;
import interswitch.academy.verve_guard.security.UserDetailsServiceImpl;
import interswitch.academy.verve_guard.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
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

        UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserByUsername(request.getEmail());
        return tokenService.issueTokens(principal);
    }

    public AuthResponse refresh(String refreshToken) {
        return tokenService.refresh(refreshToken);
    }

    public void logout(String accessToken, String refreshToken) {
        tokenService.revoke(accessToken, refreshToken);
    }

    public void logoutAll(String accessToken) {
        String userId = // extract from security context
        tokenService.revokeAll(userId);
    }
}