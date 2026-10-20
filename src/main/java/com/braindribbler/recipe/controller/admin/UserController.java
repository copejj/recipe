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

    /*
     * @GetMapping
     * public String showUserManagementDashboard(Model
     * model, @AuthenticationPrincipal UserDetails loggedInUser) {
     * List<User> chefs = userRepository.findAll();
     * 
     * // Calculate the logged-in admin's highest rank score
     * int adminMaxRank = 0;
     * User admin =
     * userRepository.findByUserAuthEmail(loggedInUser.getUsername()).orElse(null);
     * if (admin != null && admin.getRoles() != null) {
     * adminMaxRank = getHighestRoleRank(admin.getRoles());
     * }
     * 
     * model.addAttribute("chefs", chefs);
     * model.addAttribute("loggedInAdminRank", adminMaxRank); // 🔑 Inject the rank
     * score into the page context
     * 
     * return "admin/users/list"; // Points to templates/admin/users/list.html
     * }
     */

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
    public String deleteUser(@PathVariable("publicId") UUID publicId, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUserByPublicId(publicId);
            redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
