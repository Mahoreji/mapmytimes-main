package in.mapmytour.auth.security;

import in.mapmytour.auth.entity.Role;
import in.mapmytour.auth.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.*;

@Data
@AllArgsConstructor
public class UserPrincipal implements OAuth2User, UserDetails {

    private String id;
    private String email;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;
    private boolean isAccountNonExpired;
    private boolean isAccountNonLocked;
    private boolean isCredentialsNonExpired;
    private boolean isEnabled;
    private Map<String, Object> attributes;

    public static UserPrincipal create(User user) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Legacy single enum role (kept for backward compatibility)
        if (user.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        }

        // Advanced RBAC roles + permissions
        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                if (role.getName() != null) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
                }
                if (role.getPermissions() != null) {
                    role.getPermissions().forEach(p -> {
                        if (p.getCode() != null) {
                            authorities.add(new SimpleGrantedAuthority("PERM_" + p.getCode()));
                        }
                    });
                }
            }
        }

        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                authorities,
                user.isAccountNonExpired(),
                user.isAccountNonLocked(),
                user.isCredentialsNonExpired(),
                user.isEnabled(),
                null
        );
    }

    public static UserPrincipal create(User user, Map<String, Object> attributes) {
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        userPrincipal.setAttributes(attributes);
        return userPrincipal;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getName() {
        return String.valueOf(id);
    }
}