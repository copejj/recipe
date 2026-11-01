package com.braindribbler.recipe.controller.admin;

import com.braindribbler.recipe.domain.recipe.Ingredient;
import com.braindribbler.recipe.domain.recipe.IngredientClassification;
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
@RequestMapping("/admin/recipes/metadata/ingredients")
@PreAuthorize("hasAuthority('can_moderate_recipe_metadata')")
public class IngredientController {

    private final IngredientService ingredientService;

    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    /**
     * Exposes classification enum values automatically to ALL GET endpoints in this
     * controller
     */
    @ModelAttribute
    public void addManagementMetadata(Model model) {
        model.addAttribute("classifications", IngredientClassification.values());
    }

    /**
     * Renders the master ingredient moderation dashboard panel view with
     * auto-matching conflict detection.
     */
    @GetMapping
    public String showModerationDashboard(Model model) {
        List<PendingIngredientProjection> pendingQueue = ingredientService.getUnverifiedIngredients();

        // Maps pending ingredient IDs to their matching verified master record IDs
        java.util.Map<Integer, Integer> conflictMap = new java.util.HashMap<>();

        for (PendingIngredientProjection pending : pendingQueue) {
            ingredientService.getVerifiedIngredientByCanonicalName(pending.getIngredientName())
                    .ifPresent(verified -> conflictMap.put(pending.getIngredientId(), verified.getIngredientId()));
        }

        model.addAttribute("pendingIngredients", pendingQueue);
        model.addAttribute("verifiedIngredients", ingredientService.getAllIngredientsSorted(null));
        model.addAttribute("groceryCategories", ingredientService.getAllGroceryCategories());
        model.addAttribute("conflictMap", conflictMap);

        return "admin/ingredients/queue";
    }

    /**
     * Handles instant inline single ingredient validation approval requests via
     * AJAX
     */
    @PostMapping("/approve/{id}")
    @ResponseBody
    public ResponseEntity<?> approveUserIngredient(
            @PathVariable("id") Integer ingredientId,
            @RequestParam(value = "updatedName", required = false) String updatedName,
            @RequestParam(value = "updatedCategoryId", required = false) Integer updatedCategoryId,
            @RequestParam(value = "classification", defaultValue = "SOLID") String classificationStr) {
        try {
            IngredientClassification classification = IngredientClassification.valueOf(classificationStr.toUpperCase());
            ingredientService.approveAndModifyIngredient(ingredientId, updatedName, updatedCategoryId, classification);
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
            return ResponseEntity.ok(Map.of("success", true, "message", "Ingredients merged successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Renders the Master Inventory List for management
     */
    @GetMapping("/list")
    public String listAllIngredients(Model model) {
        model.addAttribute("ingredients", ingredientService.getAllIngredientsSorted(null));
        return "admin/ingredients/list";
    }

    /**
     * Displays the Edit Form for a specific existing ingredient
     */
    @GetMapping("/edit/{id}")
    public String showEditIngredientForm(@PathVariable("id") Integer id, Model model) {
        Ingredient ingredient = ingredientService.getIngredientById(id);
        model.addAttribute("ingredient", ingredient);
        model.addAttribute("groceryCategories", ingredientService.getAllGroceryCategories());
        return "admin/ingredients/edit";
    }

    /**
     * Processes updates to an existing ingredient
     */
    @PostMapping("/edit/{id}")
    public String updateIngredient(@PathVariable("id") Integer id,
            @RequestParam("ingredientName") String ingredientName,
            @RequestParam("groceryCategoryId") Integer groceryCategoryId,
            @RequestParam("classification") String classificationStr) {
        try {
            IngredientClassification classification = IngredientClassification.valueOf(classificationStr.toUpperCase());
            ingredientService.approveAndModifyIngredient(id, ingredientName, groceryCategoryId, classification);
            return "redirect:/admin/recipes/metadata/ingredients/list?updated=true";
        } catch (Exception e) {
            return "redirect:/admin/recipes/metadata/ingredients/list?error=updateFailed";
        }
    }

    /**
     * Hard-deletes an ingredient from the database pool
     */
    @PostMapping("/delete/{id}")
    public String deleteIngredient(@PathVariable("id") Integer id) {
        try {
            ingredientService.deleteIngredientById(id);
            return "redirect:/admin/recipes/metadata/ingredients/list?deleted=true";
        } catch (IllegalStateException e) {
            return "redirect:/admin/recipes/metadata/ingredients/list?error=inUse";
        } catch (Exception e) {
            return "redirect:/admin/recipes/metadata/ingredients/list?error=deleteFailed";
        }
    }

    /**
     * Displays a blank form to manually create a new globally verified ingredient
     */
    @GetMapping("/add")
    public String showAddIngredientForm(Model model) {
        model.addAttribute("ingredient", new com.braindribbler.recipe.domain.recipe.Ingredient());
        model.addAttribute("groceryCategories", ingredientService.getAllGroceryCategoriesSortedByOrder());
        return "admin/ingredients/edit";
    }

    /**
     * Processes manual ingredient creations from the admin desk
     */
    @PostMapping("/add")
    public String manuallyCreateIngredient(@RequestParam("ingredientName") String name,
            @RequestParam("groceryCategoryId") Integer categoryId,
            @RequestParam("classification") String classificationStr) {
        try {
            IngredientClassification classification = IngredientClassification.valueOf(classificationStr.toUpperCase());
            ingredientService.registerVerifiedIngredient(name, categoryId, classification);
            return "redirect:/admin/recipes/metadata/ingredients/list?created=true";
        } catch (Exception e) {
            return "redirect:/admin/recipes/metadata/ingredients/list?error=createFailed";
        }
    }

    /**
     * Retracts an approved ingredient and places it back into the moderation queue
     */
    @PostMapping("/retract/{id}")
    public String retractApprovedIngredient(@PathVariable("id") Integer id) {
        try {
            ingredientService.retractIngredientFromGlobalPool(id);
            return "redirect:/admin/recipes/metadata/ingredients/list?retracted=true";
        } catch (Exception e) {
            return "redirect:/admin/recipes/metadata/ingredients/list?error=retractFailed";
        }
    }

    /**
     * Handles asynchronous requests to hide an ingredient from the moderation queue
     * and lock it from user edits.
     */
    @PostMapping("/lock/{id}")
    @ResponseBody
    public ResponseEntity<?> lockAndHideUserIngredient(@PathVariable("id") Integer ingredientId) {
        try {
            ingredientService.lockAndHideIngredient(ingredientId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Ingredient hidden and locked successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()));
        }
    }
}
