package com.braindribbler.recipe.controller.auth;

import com.braindribbler.recipe.service.auth.PasswordManagementService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/password") // Extracts the common parent path
public class PasswordResetController {

    private final PasswordManagementService passwordService;

    @Value("${app.api.host}")
    private String appApiHost;

    public PasswordResetController(PasswordManagementService passwordService) {
        this.passwordService = passwordService;
    }

    // 1. Display Forgot Password Request Form -> Maps to /password/forgot
    @GetMapping("/forgot")
    public String showForgotPasswordForm() {
        return "auth/password/forgot";
    }

    // 2. Process Request and Dispatch Email safely -> Maps to /password/forgot
    @PostMapping("/forgot")
    public String handleForgotPassword(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
        // Runs the transactional generation and safely try-catches the Mailgun engine
        passwordService.initiatePasswordReset(email, appApiHost);

        // Return a uniform generic success message to shield against account
        // enumeration sniffing
        redirectAttributes.addFlashAttribute("successMessage",
                "If an account matches that address, a secure reset link has been dispatched to your inbox.");
        return "redirect:/password/forgot"; // Keeping your full redirect path intact
    }

    // 3. Display Reset Password Form -> Maps to /password/reset
    @GetMapping("/reset")
    public String showResetPasswordForm(@RequestParam("token") UUID token, Model model) {
        model.addAttribute("token", token);
        return "auth/password/reset";
    }

    // 4. Process Form Payload and Commit New Password Hash -> Maps to
    // /password/reset
    @PostMapping("/reset")
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
            return "redirect:/password/forgot"; // Keeping your full redirect path intact
        }

        redirectAttributes.addFlashAttribute("success", "true"); // Passes param.success payload cleanly into login view
                                                                 // matrix logic
        return "redirect:/login?verified";
    }
}
