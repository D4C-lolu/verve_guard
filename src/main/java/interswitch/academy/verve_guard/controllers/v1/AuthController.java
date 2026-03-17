package interswitch.academy.verve_guard.controllers.v1;

import interswitch.academy.verve_guard.models.request.LoginRequest;
import interswitch.academy.verve_guard.models.response.AuthResponse;
import interswitch.academy.verve_guard.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private static final String authHeader = "Authorization";
    private static final String refreshTokenHeader = "Refresh-Token";

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("login")
    public AuthResponse login(@RequestBody @Valid LoginRequest request) {
        return authService.login(request);
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("refresh")
    public AuthResponse refresh(@RequestHeader(refreshTokenHeader) String refreshToken) {
        return authService.refresh(refreshToken);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("logout")
    public void logout(
            @RequestHeader(authHeader) String accessTokenHeader,
            @RequestHeader(refreshTokenHeader) String refreshToken
    ) {
        authService.logout(accessTokenHeader, refreshToken);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("logout-all")
    public void logoutAll() {
        authService.logoutAll();
    }
}