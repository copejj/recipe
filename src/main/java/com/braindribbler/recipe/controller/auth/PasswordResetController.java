package com.braindribbler.recipe.controller.auth;

import com.braindribbler.recipe.service.auth.PasswordManagementService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
public class PasswordResetController {

    private final PasswordManagementService passwordService;

    @Value("${app.api.host}")
    private String appApiHost;

    public PasswordResetController(PasswordManagementService passwordService) {
        this.passwordService = passwordService;
    }

    // 1. Display Forgot Password Request Form
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "auth/forgot-password";
    }

    // 2. Process Request and Dispatch Email safely
    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
        // Runs the transactional generation and safely try-catches the Mailgun engine
        passwordService.initiatePasswordReset(email, appApiHost);

        // Return a uniform generic success message to shield against account
        // enumeration sniffing
        redirectAttributes.addFlashAttribute("successMessage",
                "If an account matches that address, a secure reset link has been dispatched to your inbox.");
        return "redirect:/forgot-password";
    }

    // 3. Display Reset Password Form (Landing page from Mailgun URL link click)
    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") UUID token, Model model) {
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    // 4. Process Form Payload and Commit New Password Hash
    @PostMapping("/reset-password")
    public String handleResetPassword(@RequestParam("token") UUID token,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (!password.equals(confirmPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("errorMessage", "Passwords do not match.");
            return "auth/reset-password";
        }

        if (password.length() < 8) {
            model.addAttribute("token", token);
            model.addAttribute("errorMessage", "Password must be at least 8 characters long.");
            return "auth/reset-password";
        }

        boolean success = passwordService.completePasswordReset(token, password);

        if (!success) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "The recovery link is invalid or has expired. Please request a new one.");
            return "redirect:/forgot-password";
        }

        redirectAttributes.addFlashAttribute("success", "true"); // Passes param.success payload cleanly into login view
                                                                 // matrix logic
        return "redirect:/login?verified";
    }
}
