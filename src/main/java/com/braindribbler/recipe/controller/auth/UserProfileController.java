package com.braindribbler.recipe.controller.auth;

import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.domain.auth.UserAuth;
import com.braindribbler.recipe.repository.auth.UserAuthRepository;
import com.braindribbler.recipe.repository.auth.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.OffsetDateTime;

@Controller
@RequestMapping("/profile")
public class UserProfileController {

    private final UserAuthRepository userAuthRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileController(UserAuthRepository userAuthRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userAuthRepository = userAuthRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 1. Display Profile Management Workspace
    @GetMapping
    public String showProfilePage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        UserAuth authObj = userAuthRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated context identity missing."));

        model.addAttribute("user", authObj.getUser());
        model.addAttribute("email", authObj.getEmail());
        return "auth/profile";
    }

    // 2. Process Core Detail Changes (First Name, Last Name, Display Name)
    @PostMapping("/details/update")
    public String updateProfileDetails(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("displayName") String displayName,
            RedirectAttributes redirectAttributes) {

        UserAuth authObj = userAuthRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        User user = authObj.getUser();

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setDisplayName(displayName);
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("infoMessage", "Profile baseline details successfully updated.");
        return "redirect:/profile";
    }

    // 3. Process Live Password Rotations safely
    @PostMapping("/password/change")
    public String updateAccountPassword(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {

        UserAuth authObj = userAuthRepository.findByEmail(userDetails.getUsername()).orElseThrow();

        // Security check: Must supply correct current password before a modification
        // can occur
        if (!passwordEncoder.matches(currentPassword, authObj.getPassword())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Verification failed: Existing password input is incorrect.");
            return "redirect:/profile";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation failed: New passwords do not match.");
            return "redirect:/profile";
        }

        if (newPassword.length() < 8) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Validation failed: Password must be at least 8 characters long.");
            return "redirect:/profile";
        }

        authObj.setPassword(passwordEncoder.encode(newPassword));
        authObj.setPasswordChangedAt(OffsetDateTime.now());
        authObj.setUpdatedAt(OffsetDateTime.now());
        userAuthRepository.save(authObj);

        redirectAttributes.addFlashAttribute("successMessage", "Security credentials upgraded successfully!");
        return "redirect:/profile";
    }
}
