package com.braindribbler.recipe.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.braindribbler.recipe.domain.auth.Permission;
import com.braindribbler.recipe.domain.auth.Role;
import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.domain.auth.UserAuth;

public class RecipeUserDetails implements UserDetails {

    private final UserAuth userAuth;

    public RecipeUserDetails(UserAuth userAuth) {
        this.userAuth = userAuth;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();

        User user = userAuth.getUser();
        if (user != null && user.getRoles() != null) {
            // 1. Loop through all roles assigned to the chef (e.g., ROLE_SUPER_ADMIN)
            for (Role role : user.getRoles()) {
                authorities.add(new SimpleGrantedAuthority(role.getRoleName()));

                // 2. Deep traverse and extract the fine-grained child permissions
                // Ensure your Role entity has a getPermissions() relationship mapped!
                if (role.getPermissions() != null) {
                    for (Permission perm : role.getPermissions()) {
                        authorities.add(new SimpleGrantedAuthority(perm.getPermissionName()));
                    }
                }
            }
        }

        return authorities;
    }

    @Override
    public String getPassword() {
        return userAuth.getPassword();
    }

    @Override
    public String getUsername() {
        return userAuth.getEmail();
    }

    public User getUserProfile() {
        return userAuth.getUser();
    }

    @Override
    public boolean isEnabled() {
        return this.userAuth.isVerified();
    }
}
