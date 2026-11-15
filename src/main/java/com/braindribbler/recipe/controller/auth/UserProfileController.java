package com.braindribbler.recipe.controller.auth;

import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.domain.auth.UserAuth;
import com.braindribbler.recipe.dto.recipe.RecipeSaveBreakdown;
import com.braindribbler.recipe.dto.recipe.RecipeStats;
import com.braindribbler.recipe.dto.recipe.SavedRecipeSummary;
import com.braindribbler.recipe.repository.auth.UserAuthRepository;
import com.braindribbler.recipe.repository.auth.UserRepository;
import com.braindribbler.recipe.repository.recipe.RecipeRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

@Controller
public class UserProfileController {

    private final UserAuthRepository userAuthRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RecipeRepository recipeRepository;

    public UserProfileController(UserAuthRepository userAuthRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RecipeRepository recipeRepository) {
        this.userAuthRepository = userAuthRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.recipeRepository = recipeRepository;
    }

    @GetMapping("/profile")
    public String showProfilePage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        UserAuth authObj = userAuthRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated context identity missing."));

        User currentUser = authObj.getUser();
        Integer userId = currentUser.getUserId();

        // Fetch recipe statistics safely for the Stats tab
        RecipeStats recipeStats = recipeRepository.getRecipeStatsByUserId(userId);
        List<RecipeSaveBreakdown> recipeBreakdown = recipeRepository.getRecipeSaveBreakdownByUserId(userId);
        List<SavedRecipeSummary> savedRecipes = recipeRepository.findSavedRecipesByUserId(userId);

        if (recipeBreakdown == null) {
            recipeBreakdown = Collections.emptyList();
        }
        if (savedRecipes == null) {
            savedRecipes = Collections.emptyList();
        }

        long totalViews = recipeBreakdown.stream()
                .mapToLong(item -> item != null && item.getViewCount() != null ? item.getViewCount() : 0L)
                .sum();

        long totalSharedViews = recipeBreakdown.stream()
                .mapToLong(item -> item != null && item.getSharedViewCount() != null ? item.getSharedViewCount() : 0L)
                .sum();

        long totalShares = recipeBreakdown.stream()
                .mapToLong(item -> item != null && item.getShareCount() != null ? item.getShareCount() : 0L)
                .sum();

        if (recipeStats != null) {
            recipeStats.setTotalViews(totalViews);
            recipeStats.setTotalSharedViews(totalSharedViews);
            recipeStats.setTotalShares(totalShares);
        }

        model.addAttribute("user", currentUser);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("email", authObj.getEmail());
        model.addAttribute("recipeStats", recipeStats);
        model.addAttribute("recipeBreakdown", recipeBreakdown);
        model.addAttribute("savedRecipes", savedRecipes);

        return "auth/profile";
    }

    /**
     * Redirects legacy /recipes/my-stats bookmarks straight to the Profile Stats tab.
     */
    @GetMapping("/recipes/my-stats")
    public String redirectLegacyMyStats() {
        return "redirect:/profile?tab=stats";
    }

    @PostMapping("/profile/details/update")
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

    @PostMapping("/profile/password/change")
    public String updateAccountPassword(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {

        UserAuth authObj = userAuthRepository.findByEmail(userDetails.getUsername()).orElseThrow();

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
