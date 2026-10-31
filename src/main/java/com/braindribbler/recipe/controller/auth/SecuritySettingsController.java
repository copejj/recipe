package com.braindribbler.recipe.controller.auth;

import com.braindribbler.recipe.service.auth.SecuritySessionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class SecuritySettingsController {

    private final SecuritySessionService sessionService;

    public SecuritySettingsController(SecuritySessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/account/logout-all")
    public String handleLogoutAllDevices(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) throws ServletException { // ◄ Added Servlet request reference
        
        if (userDetails != null) {
            // 1. Invalidate all device tokens in the Postgres DB
            sessionService.logoutAllDevices(userDetails.getUsername());
        }
        
        // 2. Clear out the active session container and drop browser cookie values safely
        request.logout(); 
        
        // 3. Clean redirect straight to the login endpoint with the success message flag active
        return "redirect:/login?logout";
    }
}
