package com.braindribbler.recipe.security;

import com.braindribbler.recipe.domain.auth.UserAuth;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.stream.Collectors;

public class RecipeUserDetails implements UserDetails {

    private final UserAuth userAuth;

    public RecipeUserDetails(UserAuth userAuth) {
        this.userAuth = userAuth;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return userAuth.getUser().getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getRoleName())) // e.g., "ROLE_SUPER_ADMIN"
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return userAuth.getPassword();
    }

    @Override
    public String getUsername() {
        return userAuth.getEmail();
    }

    public com.braindribbler.recipe.domain.auth.User getUserProfile() {
        return userAuth.getUser();
    }
}
