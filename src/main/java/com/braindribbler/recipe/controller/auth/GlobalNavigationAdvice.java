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

        // Dynamically build the menu structure only if the user holds authority
        if (auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("can_manage_users"))) {

            menuItems.add(new MenuItem("Users Panel", "/admin/users"));
            menuItems.add(new MenuItem("Role Customization", "/admin/roles"));
            menuItems.add(new MenuItem("Login History", "/admin/login-history"));

            // Future slots can slide in smoothly right here as you build them:
            // menuItems.add(new MenuItem("Recipe Moderation", "/admin/recipes"));
        }

        return menuItems;
    }
}
