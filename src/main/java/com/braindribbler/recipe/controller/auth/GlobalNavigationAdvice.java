package com.braindribbler.recipe.controller.auth;

import com.braindribbler.recipe.domain.auth.MenuItem;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class GlobalNavigationAdvice {

    @ModelAttribute("adminMenuItems")
    public List<MenuItem> addAdminMenuToModel() {
        List<MenuItem> menuItems = new ArrayList<>();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getAuthorities() != null) {

            java.util.Set<String> authorities = auth.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .collect(java.util.stream.Collectors.toSet());

            if (authorities.contains("can_manage_users")) {
                menuItems.add(new MenuItem("Users Panel", "/admin/users"));
                menuItems.add(new MenuItem("Login History", "/admin/login-history"));
            }

            if (authorities.contains("can_manage_roles")) {
                menuItems.add(new MenuItem("Role Customization", "/admin/roles"));
            }
        }

        return menuItems;
    }
}
