package com.braindribbler.recipe.controller.admin;

import com.braindribbler.recipe.domain.auth.Role;
import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.repository.auth.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;

    public AdminUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public String showUserManagementDashboard(Model model, @AuthenticationPrincipal UserDetails loggedInUser) {
        List<User> chefs = userRepository.findAll();

        // Calculate the logged-in admin's highest rank score
        int adminMaxRank = 0;
        User admin = userRepository.findByUserAuthEmail(loggedInUser.getUsername()).orElse(null);
        if (admin != null && admin.getRoles() != null) {
            adminMaxRank = getHighestRoleRank(admin.getRoles());
        }

        model.addAttribute("chefs", chefs);
        model.addAttribute("loggedInAdminRank", adminMaxRank); // 🔑 Inject the rank score into the page context

        return "admin/users";
    }

    private int getHighestRoleRank(Set<Role> roles) {
        if (roles == null || roles.isEmpty())
            return 0;
        int max = 0;
        for (Role r : roles) {
            int rank = getRoleRankValue(r.getRoleName());
            if (rank > max)
                max = rank;
        }
        return max;
    }

    private int getRoleRankValue(String roleName) {
        switch (roleName) {
            case "ROLE_SUPER_ADMIN":
                return 3;
            case "ROLE_ADMIN":
                return 2;
            case "ROLE_MODERATOR":
                return 1;
            default:
                return 0; // ROLE_USER
        }
    }
}
