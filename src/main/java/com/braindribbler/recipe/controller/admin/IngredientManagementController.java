package com.braindribbler.recipe.controller.admin;

import com.braindribbler.recipe.dto.recipe.PendingIngredientProjection;
import com.braindribbler.recipe.service.recipe.IngredientService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/ingredients")
// Secure the entire controller class at the gateway layer using your newly
// deployed privilege string
@PreAuthorize("hasAuthority('can_moderate_ingredients')")
public class IngredientManagementController {

    private final IngredientService ingredientService;

    public IngredientManagementController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    /**
     * Renders the master ingredient moderation dashboard panel view
     */
    @GetMapping
    public String showModerationDashboard(Model model) {
        List<PendingIngredientProjection> pendingQueue = ingredientService.getUnverifiedIngredients();
        model.addAttribute("pendingIngredients", pendingQueue);
        return "admin/ingredients-dashboard"; // Maps cleanly to your upcoming Thymeleaf template path
    }

    /**
     * Handles instant inline single ingredient validation approval requests via
     * AJAX
     */
    @PostMapping("/approve/{id}")
    @ResponseBody
    public ResponseEntity<?> approveUserIngredient(@PathVariable("id") Integer ingredientId) {
        try {
            ingredientService.approveIngredient(ingredientId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Ingredient verified successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Handles merging a duplicate submission into a valid target ingredient and
     * purging the duplicate
     */
    @PostMapping("/merge")
    @ResponseBody
    public ResponseEntity<?> mergeDuplicateIngredient(
            @RequestParam("duplicateId") Integer duplicateId,
            @RequestParam("targetId") Integer targetId) {
        try {
            ingredientService.mergeAndRejectIngredient(duplicateId, targetId);
            return ResponseEntity
                    .ok(Map.of("success", true, "message", "Ingredients merged and duplicate cleared successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
