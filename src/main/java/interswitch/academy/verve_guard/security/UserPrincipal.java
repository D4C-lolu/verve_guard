package interswitch.academy.verve_guard.security;

import interswitch.academy.verve_guard.entities.User;
import interswitch.academy.verve_guard.models.enums.UserStatus;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class UserPrincipal implements UserDetails {

    private final User user;

    public String getId() {
        return user.getId();
    }

    @Override
    @NonNull
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    @NonNull
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName()));
        user.getRole().getPermissions().forEach(p ->
                authorities.add(new SimpleGrantedAuthority(p.getName()))
        );
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !user.isDeleted() && user.getUserStatus() != UserStatus.SUSPENDED;
    }

    @Override
    public boolean isEnabled() {
        return !user.isDeleted() && user.getUserStatus() == UserStatus.ACTIVE;
    }
}
