package com.braindribbler.recipe.controller.auth;

import com.braindribbler.recipe.domain.auth.Permission;
import com.braindribbler.recipe.domain.auth.Role;
import com.braindribbler.recipe.repository.auth.PermissionRepository;
import com.braindribbler.recipe.repository.auth.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;

@Controller
@RequestMapping("/admin/roles")
@PreAuthorize("hasAuthority('can_manage_users')") // Ensures endpoint security
public class RoleManagementController {

    private final RoleRepository roleRepository;

    private final PermissionRepository permissionRepository;

    RoleManagementController(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @GetMapping
    public String listRoles(Model model) {
        model.addAttribute("roles", roleRepository.findAll());
        return "admin/roles-list";
    }

    @GetMapping("/edit/{id}")
    public String editRoleForm(@PathVariable Integer id, Model model) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid role ID: " + id));

        model.addAttribute("role", role);
        model.addAttribute("allPermissions", permissionRepository.findAll());
        return "admin/role-edit";
    }

    @PostMapping("/edit/{id}")
    public String updateRolePermissions(
            @PathVariable Integer id,
            @RequestParam(name = "permissionIds", required = false) List<Integer> permissionIds) {

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid role ID: " + id));

        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<Permission> chosenPermissions = permissionRepository.findAllById(permissionIds);
            role.setPermissions(new HashSet<>(chosenPermissions));
        } else {
            role.getPermissions().clear(); // Safe-guard clear if no checkboxes are checked
        }

        roleRepository.save(role);
        return "redirect:/admin/roles";
    }
}
