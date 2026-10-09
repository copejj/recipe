package com.braindribbler.recipe.controller.auth;

import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.dto.auth.RegistrationDto;
import com.braindribbler.recipe.service.auth.UserRegistrationService;

import jakarta.validation.Valid;

import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserRegistrationService userRegistrationService;

    public AuthController(UserRegistrationService userRegistrationService) {
        this.userRegistrationService = userRegistrationService;
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "login"; // Points to templates/login.html
    }

    @GetMapping("/register")
    public String showRegistrationPage(Model model) {
        // Binds an empty DTO to the form structure so Thymeleaf can capture fields
        // cleanly
        model.addAttribute("registrationDto", new RegistrationDto());
        return "register"; // Points to templates/register.html
    }

    @PostMapping("/register")
    public String registerUserAccount(
            @Valid @ModelAttribute("registrationDto") RegistrationDto registrationDto,
            BindingResult result,
            Model model) {

        // 1. If any JSR-303 field validation criteria fail, instantly reject and return
        // the form
        if (result.hasErrors()) {
            return "register";
        }

        try {
            // 2. Map DTO input strings directly to your core service transaction layer
            User newUser = userRegistrationService.registerNewUser(
                    registrationDto.getEmail(),
                    registrationDto.getPassword(),
                    registrationDto.getFirstName(),
                    registrationDto.getLastName(),
                    registrationDto.getDisplayName(),
                    null // Passing null defaults standard signups to ROLE_USER inside the service layer
            );

            userRegistrationService.sendVerificationEmail(newUser);

            // 3. Redirect back to login with a URL success flag
            return "redirect:/login?success";

        } catch (IllegalArgumentException e) {
            // 4. Catch domain errors (like duplicate emails) and bind them back as global
            // form fields
            result.rejectValue("email", "error.registrationDto", e.getMessage());
            return "register";
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
            model.addAttribute("verificationError", e.getMessage());
            return "verification-failed"; // Points to templates/verification-failed.html
        }
    }
}
