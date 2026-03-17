package interswitch.academy.verve_guard.util;

import interswitch.academy.verve_guard.exceptions.BadRequestException;
import interswitch.academy.verve_guard.exceptions.UnauthorizedException;
import interswitch.academy.verve_guard.security.UserPrincipal;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class SecurityUtil {

    public static Optional<String> findCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        if (!(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            return Optional.empty();
        }

        return Optional.of(userPrincipal.getUser().getId());
    }

    public static String getCurrentUserId() {
        return findCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Unauthorized"));
    }

    public static String extractToken(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new BadRequestException("Invalid authorization header format");
        }

        String token = bearerToken.stripLeading();

        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).stripLeading();
            System.out.println("Token had bearer. Strange innit");
        }

        if (token.isBlank()) {
            throw new BadRequestException("Invalid authorization header format");
        }

        return token;
    }
}
