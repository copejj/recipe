package com.braindribbler.recipe.controller.auth;

import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.dto.auth.RegistrationDto;
import com.braindribbler.recipe.service.auth.UserRegistrationService;

import jakarta.validation.Valid;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final UserRegistrationService userRegistrationService;

    public AuthController(UserRegistrationService userRegistrationService) {
        this.userRegistrationService = userRegistrationService;
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "auth/login"; // Points to templates/login.html
    }

    @GetMapping("/register")
    public String showRegistrationPage(Model model) {
        // Binds an empty DTO to the form structure so Thymeleaf can capture fields
        // cleanly
        model.addAttribute("registrationDto", new RegistrationDto());
        return "auth/register"; // Points to templates/register.html
    }

    @PostMapping("/register")
    public String registerUserAccount(
            @Valid @ModelAttribute("registrationDto") RegistrationDto registrationDto,
            BindingResult result,
            @RequestParam(value = "website", required = false) String honeypot,
            Model model) {

        // 1. Honeypot check
        if (honeypot != null && !honeypot.isBlank()) {
            log.warn("Honeypot triggered! Suppressed automated registration attempt from address: {}",
                    registrationDto.getEmail());
            return "redirect:/login?success";
        }

        // NEW ADDITION: Check if password values are distinct from one another
        if (registrationDto.getPassword() != null && registrationDto.getConfirmPassword() != null) {
            if (!registrationDto.getPassword().equals(registrationDto.getConfirmPassword())) {
                result.rejectValue("confirmPassword", "error.registrationDto", "Passwords do not match.");
            }
        }

        // 2. If any validation criteria fail, return the registration form cleanly
        if (result.hasErrors()) {
            return "auth/register";
        }

        try {
            // 3. Map DTO input strings directly to your core service layer
            User newUser = userRegistrationService.registerNewUser(
                    registrationDto.getEmail(),
                    registrationDto.getPassword(),
                    registrationDto.getFirstName(),
                    registrationDto.getLastName(),
                    registrationDto.getDisplayName(),
                    null
            );

            userRegistrationService.sendVerificationEmail(newUser);
            return "redirect:/login?success";

        } catch (IllegalArgumentException e) {
            result.rejectValue("email", "error.registrationDto", e.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/verify")
    public String verifyUserAccount(@RequestParam("token") UUID token, Model model) {
        try {
            // Delegate verification transaction logic to your service layer
            userRegistrationService.verifyUserToken(token);

            // Redirect to login with a dedicated verification success query parameter flag
            return "redirect:/login?verified";

        } catch (IllegalArgumentException e) {
            // Catch invalid or expired token issues and pass the error message to the model
            model.addAttribute("errorTitle", "Whoops! Verification Failed");
            model.addAttribute("errorCode", "401");
            model.addAttribute("errorMessage", e.getMessage());
            return "error"; // Points to templates/verification-failed.html
        }
    }
}
