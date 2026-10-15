package com.braindribbler.recipe.controller.admin;

import com.braindribbler.recipe.domain.recipe.Ingredient;
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
        model.addAttribute("verifiedIngredients", ingredientService.getAllIngredientsSorted());
        model.addAttribute("groceryCategories", ingredientService.getAllGroceryCategories());
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
            @RequestParam(value = "updatedCategoryId", required = false) Integer updatedCategoryId) {
        try {
            ingredientService.approveAndModifyIngredient(ingredientId, updatedName, updatedCategoryId);
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
        model.addAttribute("ingredients", ingredientService.getAllIngredientsSorted());
        return "admin/ingredients/list";
    }

    /**
     * Displays the Edit Form for a specific existing ingredient
     */
    @GetMapping("/edit/{id}")
    public String showEditIngredientForm(@PathVariable("id") Integer id, Model model) {
        // 🔥 FIXED: Routes request through service instead of raw repository pull
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
            @RequestParam("groceryCategoryId") Integer groceryCategoryId) {
        try {
            ingredientService.approveAndModifyIngredient(id, ingredientName, groceryCategoryId);
            return "redirect:/admin/ingredients/list?updated=true";
        } catch (Exception e) {
            return "redirect:/admin/ingredients/list?error=updateFailed";
        }
    }

    /**
     * Hard-deletes an ingredient from the database pool
     */
    @PostMapping("/delete/{id}")
    public String deleteIngredient(@PathVariable("id") Integer id) {
        try {
            ingredientService.deleteIngredientById(id);
            return "redirect:/admin/ingredients/list?deleted=true";
        } catch (IllegalStateException e) {
            // Redirects with the specific safety usage restriction string to warn the admin
            return "redirect:/admin/ingredients/list?error=inUse";
        } catch (Exception e) {
            return "redirect:/admin/ingredients/list?error=deleteFailed";
        }
    }

    /**
     * Renders the high-level Executive Metrics Control Dashboard Center
     * Maps perfectly to: GET /admin/ingredients/dashboard
     */
    @GetMapping("/dashboard")
    public String showExecutiveDashboard(Model model) {
        // 1. Fetch counts to fuel our high-visibility layout summary widget metrics
        int pendingItems = ingredientService.getUnverifiedIngredients().size();
        int verifiedItems = ingredientService.getAllIngredientsSorted().size();
        int groceryAisles = ingredientService.getAllGroceryCategories().size();

        model.addAttribute("pendingCount", pendingItems);
        model.addAttribute("totalCount", verifiedItems);
        model.addAttribute("categoryCount", groceryAisles);

        return "admin/ingredients/dashboard"; // Points directly to your new metrics hub!
    }

    /**
     * Displays a blank form to manually create a new globally verified ingredient
     * Maps to: GET /admin/ingredients/add
     */
    @GetMapping("/add")
    public String showAddIngredientForm(Model model) {
        model.addAttribute("ingredient", new com.braindribbler.recipe.domain.recipe.Ingredient());
        model.addAttribute("groceryCategories", ingredientService.getAllGroceryCategoriesSortedByOrder());
        return "admin/ingredients/edit"; // Reuses the edit.html form view blueprint for creation profiles!
    }

    /**
     * Processes manual ingredient creations from the admin desk
     * Maps to: POST /admin/ingredients/add
     */
    @PostMapping("/add")
    public String manuallyCreateIngredient(@RequestParam("ingredientName") String name,
            @RequestParam("groceryCategoryId") Integer categoryId) {
        try {
            // Registers item directly flipping is_verified = TRUE instantly
            ingredientService.registerVerifiedIngredient(name, categoryId);
            return "redirect:/admin/ingredients/list?created=true";
        } catch (Exception e) {
            return "redirect:/admin/ingredients/list?error=createFailed";
        }
    }

    /**
     * Retracts an approved ingredient and places it back into the moderation queue
     * Maps to: POST /admin/ingredients/retract/{id}
     */
    @PostMapping("/retract/{id}") // ◄ Renamed from /delete/{id}
    public String retractApprovedIngredient(@PathVariable("id") Integer id) {
        try {
            ingredientService.retractIngredientFromGlobalPool(id);
            return "redirect:/admin/ingredients/list?retracted=true";
        } catch (Exception e) {
            return "redirect:/admin/ingredients/list?error=retractFailed";
        }
    }
}
