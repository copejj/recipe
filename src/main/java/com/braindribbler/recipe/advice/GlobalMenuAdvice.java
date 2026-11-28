package com.braindribbler.recipe.advice;

import com.braindribbler.recipe.domain.auth.MenuItem;
import com.braindribbler.recipe.service.feedback.FeedbackAndChangelogService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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

    private final FeedbackAndChangelogService feedbackService;

    public GlobalMenuAdvice(FeedbackAndChangelogService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @ModelAttribute("latestChangelogId")
    public Integer addLatestChangelogId() {
        return feedbackService.getLatestPublishedChangelogId();
    }

    @ModelAttribute("globalMenuItems")
    public List<MenuItem> addGlobalMenuToModel() {
        List<MenuItem> menuItems = new ArrayList<>();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        menuItems.add(new MenuItem("Home", "/"));
        menuItems.add(new MenuItem("Search", "/search"));

        // 1. Handle Guest User (Not authenticated or anonymous)
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            menuItems.add(new MenuItem("Register", "/register", "nav-button nav-link--cta"));
            return menuItems;
        }

        // 2. Handle Logged-In Authenticated User Base Links
        menuItems.add(new MenuItem("Profile", "/profile"));

        // 3. Collect Authorities for Permission-Based Links
        if (auth.getAuthorities() != null) {
            Set<String> authorities = auth.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .collect(Collectors.toSet());

            boolean hasAdmin = !Collections.disjoint(authorities, Set.of(
                "can_moderate_recipe_metadata",
                "can_manage_users",
                "can_manage_roles",
                "can_view_audit_logs",
                "can_manage_migrations",
                "can_view_errors",
                "can_manage_messages"
            ));

            if (hasAdmin) {
                menuItems.add(new MenuItem("Admin", "/admin"));
            }
        }

        return menuItems;
    }
}
