package interswitch.academy.verve_guard.util;

import interswitch.academy.verve_guard.exceptions.UnauthenticatedException;
import interswitch.academy.verve_guard.security.UserPrincipal;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {
    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthenticatedException("User not authenticated");
        }

        if (!(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new UnauthenticatedException("User not authenticated");
        }

        return userPrincipal.getUser().getId();
    }
}
