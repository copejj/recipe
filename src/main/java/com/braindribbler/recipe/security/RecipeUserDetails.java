package com.braindribbler.recipe.security;

import com.braindribbler.recipe.domain.auth.UserAuth;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

public class RecipeUserDetails implements UserDetails {

    private final UserAuth userAuth;

    public RecipeUserDetails(UserAuth userAuth) {
        this.userAuth = userAuth;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Fallback role assignment matching your transient defaults
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return userAuth.getPassword();
    }

    @Override
    public String getUsername() {
        return userAuth.getEmail();
    } // Uses email as the formal login username credential

    public com.braindribbler.recipe.domain.auth.User getUserProfile() {
        return userAuth.getUser();
    }
}
