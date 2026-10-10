package com.braindribbler.recipe.controller.auth;

import com.braindribbler.recipe.domain.auth.Permission;
import com.braindribbler.recipe.domain.auth.Role;
import com.braindribbler.recipe.repository.auth.PermissionRepository;
import com.braindribbler.recipe.repository.auth.RoleRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.HashSet;
import java.util.List;

@Controller
@RequestMapping("/admin/roles")
@PreAuthorize("hasAuthority('can_manage_users')")
public class RoleManagementController {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleManagementController(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @GetMapping
    public String listRoles(Model model) {
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("allPermissions", permissionRepository.findAll());
        return "admin/roles-list";
    }

    // 1. Process New Role Creation
    @PostMapping("/create")
    public String createRole(@RequestParam String roleName, @RequestParam String description) {
        Role role = new Role();
        // Standardize Spring Security role naming conventions if missing
        if (!roleName.startsWith("ROLE_")) {
            roleName = "ROLE_" + roleName.toUpperCase();
        }
        role.setRoleName(roleName);
        role.setDescription(description);
        roleRepository.save(role);
        return "redirect:/admin/roles";
    }

    // 2. Process New Permission Creation
    @PostMapping("/permissions/create")
    public String createPermission(@RequestParam String permissionName, @RequestParam String description) {
        Permission permission = new Permission();
        permission.setPermissionName(permissionName.toLowerCase().replace(" ", "_"));
        permission.setDescription(description);
        permissionRepository.save(permission);
        return "redirect:/admin/roles";
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
    public String updateRolePermissions(@PathVariable Integer id,
            @RequestParam(required = false) List<Integer> permissionIds) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid role ID: " + id));
        if (permissionIds != null && !permissionIds.isEmpty()) {
            role.setPermissions(new HashSet<>(permissionRepository.findAllById(permissionIds)));
        } else {
            role.getPermissions().clear();
        }
        roleRepository.save(role);
        return "redirect:/admin/roles";
    }
}
