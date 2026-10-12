package com.braindribbler.recipe.controller.auth;

import com.braindribbler.recipe.domain.auth.MenuItem;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalNavigationAdvice {

    @ModelAttribute("globalMenuItems")
    public List<MenuItem> addGlobalMenuToModel() {
        List<MenuItem> menuItems = new ArrayList<>();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 1. Handle Guest User (Not authenticated or anonymous)
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            menuItems.add(new MenuItem("Home", "/"));
            menuItems.add(new MenuItem("Search", "/search"));
            return menuItems; // Exit early for guests
        }

        // 2. Handle Logged-In Authenticated User Base Links
        menuItems.add(new MenuItem("Home", "/dashboard"));
        menuItems.add(new MenuItem("Search", "/search"));
        menuItems.add(new MenuItem("My Profile", "/profile"));

        // 3. Collect Authorities for Permission-Based Links
        if (auth.getAuthorities() != null) {
            Set<String> authorities = auth.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .collect(Collectors.toSet());

            // Build dynamic child options for Admin sub-menu
            List<MenuItem> adminChildren = new ArrayList<>();

            if (authorities.contains("can_manage_users")) {
                adminChildren.add(new MenuItem("Users Panel", "/admin/users"));
                adminChildren.add(new MenuItem("Login History", "/admin/login-history"));
            }
            if (authorities.contains("can_manage_roles")) {
                adminChildren.add(new MenuItem("Role Customization", "/admin/roles"));
            }
            if (authorities.contains("can_manage_migrations")) {
                adminChildren.add(new MenuItem("Migration History", "/admin/migrations"));
            }

            // Only add the Admin dropdown if the user has access to at least one child
            // action
            if (!adminChildren.isEmpty()) {
                menuItems.add(new MenuItem("Admin", adminChildren));
            }
        }

        return menuItems;
    }
}
