package com.braindribbler.recipe.controller.recipe;

import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.domain.recipe.GroceryCategory;
import com.braindribbler.recipe.domain.recipe.Ingredient;
import com.braindribbler.recipe.domain.recipe.IngredientClassification;
import com.braindribbler.recipe.security.RecipeUserDetails;
import com.braindribbler.recipe.service.recipe.IngredientService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/ingredients")
public class IngredientController {

    private final IngredientService ingredientService;

    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    /**
     * Handles inline AJAX quick-adds for missing ingredients during recipe
     * construction.
     * Maps to: POST /ingredients/quick-add
     */
    @PostMapping("/quick-add")
    @ResponseBody
    public ResponseEntity<?> quickAddIngredient(@RequestParam("name") String ingredientName,
            @RequestParam("groceryCategoryId") Integer groceryCategoryId,
            @RequestParam(value = "classification", defaultValue = "SOLID") String classificationStr,
            @AuthenticationPrincipal RecipeUserDetails currentUser) {
        try {
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "Session expired."));
            }
            if (ingredientName == null || ingredientName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Name cannot be empty."));
            }

            // Convert string to enum safely
            com.braindribbler.recipe.domain.recipe.IngredientClassification classification;
            try {
                classification = com.braindribbler.recipe.domain.recipe.IngredientClassification
                        .valueOf(classificationStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Invalid classification type."));
            }

            User userEntity = currentUser.getUserProfile();

            // Pass classification down to the updated service method
            Ingredient saved = ingredientService.quickRegisterIngredient(ingredientName, groceryCategoryId,
                    classification, userEntity);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "ingredientId", saved.getIngredientId(),
                    "ingredientName", saved.getIngredientName()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "An error occurred."));
        }
    }

    @GetMapping("/my-fresh")
    public String viewMyFreshIngredients(Model model, @AuthenticationPrincipal RecipeUserDetails currentUser) {
        if (currentUser == null) {
            return "redirect:/login";
        }

        Integer userId = currentUser.getUserProfile().getUserId();
        List<Ingredient> myPendingList = ingredientService.getUnverifiedIngredientsByUser(userId);

        // ADD THESE REFERENCE ENTITY LOOKUPS TO POPULATE INLINE SELECTIONS
        model.addAttribute("myFreshIngredients", myPendingList);
        model.addAttribute("groceryCategories", ingredientService.getAllGroceryCategories());
        model.addAttribute("classifications", IngredientClassification.values()); // Exposes Enum configurations

        return "ingredients/my-fresh";
    }

    @GetMapping("/edit/{id}")
    public String editMyFreshIngredientForm(@PathVariable("id") Integer id, Model model,
            @AuthenticationPrincipal RecipeUserDetails currentUser) {
        if (currentUser == null)
            return "redirect:/login";

        Ingredient ingredient = ingredientService.getIngredientById(id);

        // Security Check: Only allow if unverified AND owned by the requester
        if (ingredient.getIsVerified()
                || !ingredient.getCreatedByUserId().equals(currentUser.getUserProfile().getUserId())) {
            return "redirect:/search?unauthorized";
        }

        // Fetch grocery categories pool to populate the dropdown selection form
        List<GroceryCategory> categories = ingredientService.getAllGroceryCategories();

        model.addAttribute("ingredient", ingredient);
        model.addAttribute("categories", categories);
        return "ingredients/edit";
    }

    // 3. POST: Save the updated ingredient name and classification updates securely
    @PostMapping("/edit/{id}")
    public String updateMyFreshIngredient(@PathVariable("id") Integer id,
            @RequestParam("ingredientName") String name,
            @RequestParam("groceryCategoryId") Integer categoryId,
            @RequestParam("classification") String classification,
            @AuthenticationPrincipal RecipeUserDetails currentUser) {
        if (currentUser == null)
            return "redirect:/login";

        ingredientService.updateUserSubmittedIngredient(id, name, categoryId, classification,
                currentUser.getUserProfile().getUserId());

        return "redirect:/ingredients/my-fresh?success";
    }

}
