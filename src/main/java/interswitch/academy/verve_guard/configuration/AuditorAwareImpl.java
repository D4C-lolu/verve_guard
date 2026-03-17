package interswitch.academy.verve_guard.configuration;

import interswitch.academy.verve_guard.util.SecurityUtil;
import lombok.NonNull;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    @NonNull
    public Optional<String> getCurrentAuditor() {
        return SecurityUtil.findCurrentUserId();
    }
}