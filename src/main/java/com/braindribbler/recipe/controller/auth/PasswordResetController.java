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
    @GetMapping("/password/forgot")
    public String showForgotPasswordForm() {
        return "auth/password/forgot";
    }

    // 2. Process Request and Dispatch Email safely
    @PostMapping("/password/forgot")
    public String handleForgotPassword(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
        // Runs the transactional generation and safely try-catches the Mailgun engine
        passwordService.initiatePasswordReset(email, appApiHost);

        // Return a uniform generic success message to shield against account
        // enumeration sniffing
        redirectAttributes.addFlashAttribute("successMessage",
                "If an account matches that address, a secure reset link has been dispatched to your inbox.");
        return "redirect:/password/forgot";
    }

    // 3. Display Reset Password Form (Landing page from Mailgun URL link click)
    @GetMapping("/password/reset")
    public String showResetPasswordForm(@RequestParam("token") UUID token, Model model) {
        model.addAttribute("token", token);
        return "auth/password/reset";
    }

    // 4. Process Form Payload and Commit New Password Hash
    @PostMapping("/password/reset")
    public String handleResetPassword(@RequestParam("token") UUID token,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (!password.equals(confirmPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("errorMessage", "Passwords do not match.");
            return "auth/password/reset";
        }

        if (password.length() < 8) {
            model.addAttribute("token", token);
            model.addAttribute("errorMessage", "Password must be at least 8 characters long.");
            return "auth/password/reset";
        }

        boolean success = passwordService.completePasswordReset(token, password);

        if (!success) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "The recovery link is invalid or has expired. Please request a new one.");
            return "redirect:/password/forgot";
        }

        redirectAttributes.addFlashAttribute("success", "true"); // Passes param.success payload cleanly into login view
                                                                 // matrix logic
        return "redirect:/login?verified";
    }
}
