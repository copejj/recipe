package com.braindribbler.recipe.controller.admin;

import com.braindribbler.recipe.domain.auth.Role;
import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.repository.auth.RoleRepository;
import com.braindribbler.recipe.repository.auth.UserRepository;
import com.braindribbler.recipe.service.auth.RoleRankCache;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/users/edit/{publicId}")
public class UserRoleController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RoleRankCache roleRankCache;

    public UserRoleController(UserRepository userRepository, RoleRepository roleRepository,
            RoleRankCache roleRankCache) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.roleRankCache = roleRankCache;
    }

    /**
     * Renders the single-user role edit form page.
     */
    @GetMapping
    public String showEditUserForm(@PathVariable("publicId") UUID publicId,
            @AuthenticationPrincipal UserDetails loggedInUser,
            Model model) {

        // 1. Fetch the target user being modified
        User targetUser = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 2. Calculate ranks to prevent lower admins from editing higher admins
        int targetUserMaxRank = getHighestRoleRank(targetUser.getRoles());
        int loggedInAdminMaxRank = getAdminRankFromUsername(loggedInUser.getUsername());

        if (targetUserMaxRank >= loggedInAdminMaxRank) {
            throw new AccessDeniedException("You do not have administrative authority to modify this user.");
        }

        // 3. Filter available roles: Only load roles strictly BELOW the logged-in
        // admin's rank
        List<Role> assignableRoles = roleRepository.findAll().stream()
                .filter(role -> getRoleRankValue(role.getRoleName()) < loggedInAdminMaxRank)
                .collect(java.util.stream.Collectors.toList()); // <-- Fixed clean termination point

        // 4. Calculate functional deletion capability flag
        boolean canDelete = false;
        if (loggedInUser != null) {
            canDelete = loggedInUser.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("can_delete_users"));
        }

        model.addAttribute("targetUser", targetUser);
        model.addAttribute("assignableRoles", assignableRoles);
        model.addAttribute("canDelete", canDelete); // Passed directly to HTML

        return "admin/users/edit"; // Points to templates/admin/user-edit.html
    }

    /**
     * Processes the submission form securely, including updating roles and account
     * suspension status.
     */
    @PostMapping("/roles")
    public String updateRoles(@PathVariable("publicId") UUID publicId,
            @RequestParam(value = "selectedRoles", required = false) Set<Integer> selectedRoleIds,
            @RequestParam(value = "inactiveStatus", required = false) Boolean inactiveStatus,
            @AuthenticationPrincipal UserDetails loggedInUser) {

        User targetUser = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        int targetUserMaxRank = getHighestRoleRank(targetUser.getRoles());
        int loggedInAdminMaxRank = getAdminRankFromUsername(loggedInUser.getUsername());

        if (targetUserMaxRank >= loggedInAdminMaxRank) {
            throw new AccessDeniedException("Unauthorized modification attempt blocked.");
        }

        /* --- MANAGE INACTIVE STATUS AND AUDIT TRAIL --- */
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean canManageUsers = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("can_manage_users"));

        if (canManageUsers) {
            if (Boolean.TRUE.equals(inactiveStatus)) {
                // If the user isn't already inactive, apply the timestamp and log who did it
                if (targetUser.getDisabledAt() == null) {
                    targetUser.setDisabledAt(OffsetDateTime.now());

                    // Fetch the current authenticated admin's User entity record
                    User currentAdmin = userRepository.findByUserAuthEmail(loggedInUser.getUsername())
                            .orElseThrow(() -> new UsernameNotFoundException("Admin not found"));
                    targetUser.setDisabledBy(currentAdmin);
                }
            } else {
                // Reactivating the user removes both the timestamp and the administrator log
                targetUser.setDisabledAt(null);
                targetUser.setDisabledBy(null);
            }
        } else if (inactiveStatus != null) {
            throw new AccessDeniedException("Security Exception: Insufficient clearance to modify user state.");
        }

        /* --- MANAGE EXISTING ROLES STRUCTURAL SECURITY --- */
        Set<Role> newRoles = selectedRoleIds == null ? Set.of()
                : roleRepository.findAllById(selectedRoleIds).stream().collect(Collectors.toSet());

        for (Role r : newRoles) {
            if (roleRankCache.getRankValue(r.getRoleName()) >= loggedInAdminMaxRank) {
                throw new AccessDeniedException("Security Alert: Escalation attempt caught.");
            }
        }

        targetUser.setRoles(newRoles);
        userRepository.save(targetUser);

        return "redirect:/admin/users?success_roles";
    }

    private int getAdminRankFromUsername(String email) {
        return userRepository.findByUserAuthEmail(email)
                .map(admin -> getHighestRoleRank(admin.getRoles()))
                .orElseThrow(
                        () -> new UsernameNotFoundException("Admin context resolution failed for email: " + email));
    }

    private int getHighestRoleRank(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return 0;
        }
        return roles.stream()
                .mapToInt(role -> roleRankCache.getRankValue(role.getRoleName())) // ⚡ Instant local lookup!
                .max()
                .orElse(0);
    }

    private int getRoleRankValue(String roleName) {
        return roleRankCache.getRankValue(roleName);
    }
}
