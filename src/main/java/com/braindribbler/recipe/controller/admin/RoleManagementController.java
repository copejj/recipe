package com.braindribbler.recipe.controller.admin;

import com.braindribbler.recipe.domain.auth.Role;
import com.braindribbler.recipe.repository.auth.PermissionRepository;
import com.braindribbler.recipe.repository.auth.RoleRepository;
import com.braindribbler.recipe.service.auth.RoleRankCache;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.HashSet;
import java.util.List;

@Controller
@RequestMapping("/admin/roles")
@PreAuthorize("hasAuthority('can_manage_roles')")
public class RoleManagementController {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRankCache roleRankCache;

    public RoleManagementController(RoleRepository roleRepository, PermissionRepository permissionRepository,
            RoleRankCache roleRankCache) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.roleRankCache = roleRankCache;
    }

    @GetMapping
    public String listRoles(Model model) {
        model.addAttribute("roles", roleRepository.findAllByOrderByRoleRankDesc());
        model.addAttribute("allPermissions", permissionRepository.findAll());
        return "admin/roles/list";
    }

    @GetMapping("/add")
    public String addRoleForm(Model model) {
        Role newRole = new Role();
        newRole.setRoleRank(0); // Default rank for new roles
        model.addAttribute("role", newRole);
        model.addAttribute("allPermissions", permissionRepository.findAll());
        return "admin/roles/edit";
    }

    @PostMapping("/add")
    public String createRole(@ModelAttribute("role") Role formRole,
                             @RequestParam(required = false) List<Integer> permissionIds) {
        Role role = new Role();
        applyRoleDetails(role, formRole, permissionIds);
        roleRepository.save(role);
        roleRankCache.refreshCache();
        return "redirect:/admin/roles?created=true";
    }

    @GetMapping("/edit/{id}")
    public String editRoleForm(@PathVariable Integer id, Model model) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid role ID: " + id));
        model.addAttribute("role", role);
        model.addAttribute("allPermissions", permissionRepository.findAll());
        return "admin/roles/edit";
    }

    @PostMapping("/edit/{id}")
    public String updateRole(@PathVariable Integer id,
                             @ModelAttribute("role") Role formRole,
                             @RequestParam(required = false) List<Integer> permissionIds) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid role ID: " + id));
        applyRoleDetails(role, formRole, permissionIds);
        roleRepository.save(role);
        roleRankCache.refreshCache();
        return "redirect:/admin/roles?updated=true";
    }

    // Shared helper to enforce naming, rank clamping, and permission assignment
    private void applyRoleDetails(Role targetRole, Role formRole, List<Integer> permissionIds) {
        // 1. Standardize ROLE_ prefix
        String formattedName = formRole.getRoleName() != null
                ? formRole.getRoleName().toUpperCase().trim()
                : "";
        if (!formattedName.startsWith("ROLE_")) {
            formattedName = "ROLE_" + formattedName;
        }
        targetRole.setRoleName(formattedName);
        targetRole.setDescription(formRole.getDescription());

        // 2. Clamp rank safely between 0 and 2 (unless editing an existing SUPER_ADMIN >= 3)
        int rank = formRole.getRoleRank() != null ? formRole.getRoleRank() : 0;
        if (targetRole.getRoleRank() == null || targetRole.getRoleRank() < 3) {
            if (rank < 0) rank = 0;
            if (rank >= 3) rank = 2;
            targetRole.setRoleRank(rank);
        }

        // 3. Assign selected permissions
        if (permissionIds != null && !permissionIds.isEmpty()) {
            targetRole.setPermissions(new HashSet<>(permissionRepository.findAllById(permissionIds)));
        } else {
            targetRole.getPermissions().clear();
        }
    }
}
