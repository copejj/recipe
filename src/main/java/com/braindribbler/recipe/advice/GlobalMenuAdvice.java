package com.braindribbler.recipe.advice;

import com.braindribbler.recipe.domain.auth.MenuItem;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalMenuAdvice {

    @ModelAttribute("globalMenuItems")
    public List<MenuItem> addGlobalMenuToModel() {
        List<MenuItem> menuItems = new ArrayList<>();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 1. Handle Guest User (Not authenticated or anonymous)
        if (auth == null || !auth.isAuthenticated() ||
                auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            menuItems.add(new MenuItem("Home", "/"));
            menuItems.add(new MenuItem("Search", "/search"));
            menuItems.add(new MenuItem("Register", "/register", "nav-button nav-link--cta"));
            return menuItems;
        }

        // 2. Handle Logged-In Authenticated User Base Links
        menuItems.add(new MenuItem("Home", "/dashboard"));
        menuItems.add(new MenuItem("Search", "/search"));
        menuItems.add(new MenuItem("Profile", "/profile"));

        // 3. Collect Authorities for Permission-Based Links
        if (auth.getAuthorities() != null) {
            Set<String> authorities = auth.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .collect(Collectors.toSet());

            // Build completely separate child options for technical System Administration
            boolean hasAdmin = !Collections.disjoint(authorities, Set.of(
                "can_moderate_recipe_metadata",
                "can_manage_users",
                "can_manage_roles",
                "can_view_audit_logs",
                "can_manage_migrations"
            ));

            if (hasAdmin) {
                menuItems.add(new MenuItem("Admin", "/admin"));
            }
        }

        return menuItems;
    }
}
