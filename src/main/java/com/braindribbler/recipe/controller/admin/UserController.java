package com.braindribbler.recipe.controller.admin;

import com.braindribbler.recipe.domain.auth.Role;
import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.repository.auth.UserRepository;
import com.braindribbler.recipe.service.admin.UserService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Controller
@RequestMapping("/admin/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String showUserManagementDashboard(Model model, @AuthenticationPrincipal UserDetails loggedInUser) {
        // Delegate all database fetch and calculation logic to the service
        List<User> chefs = userService.getAllUsers();
        int adminMaxRank = userService.getHighestRoleRankForUserEmail(loggedInUser.getUsername());

        model.addAttribute("chefs", chefs);
        model.addAttribute("loggedInAdminRank", adminMaxRank);

        return "admin/users/list";
    }

    /**
     * Processes the submission form securely, removes user and cascades
     * through the different tables
     */
    @PostMapping("/delete/{publicId}")
    @PreAuthorize("hasAuthority('can_delete_users')")
    public String deleteUser(
            @PathVariable("publicId") UUID publicId,
            @AuthenticationPrincipal UserDetails loggedInUser,
            RedirectAttributes redirectAttributes) {
        try {
            // 1. Calculate active operating authority constraints
            int adminMaxRank = userService.getHighestRoleRankForUserEmail(loggedInUser.getUsername());

            // 2. Fetch the metadata configuration context of the target account
            User targetUser = userService.getUserByPublicId(publicId);

            // 3. Defensive Validation: Enforce structural business boundaries
            if (targetUser.getHighestRoleRankValue() >= adminMaxRank) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Access Denied: You cannot delete a user with an equal or higher administrative rank tier.");
                return "redirect:/admin/users";
            }

            // 4. Safe structural execution path
            userService.deleteUserByPublicId(publicId);
            redirectAttributes.addFlashAttribute("successMessage", "Chef account removed from registry successfully.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error handling data deletion requests: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
