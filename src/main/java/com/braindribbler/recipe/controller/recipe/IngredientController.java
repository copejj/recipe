package com.braindribbler.recipe.controller.recipe;

import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.domain.recipe.Ingredient;
import com.braindribbler.recipe.security.RecipeUserDetails;
import com.braindribbler.recipe.service.recipe.IngredientService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
public class IngredientController {

    private final IngredientService ingredientService;

    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    /**
     * Handles inline AJAX quick-adds for missing ingredients during recipe
     * construction.
     * Maps to: POST /recipes/ingredients/quick-add
     */
    @PostMapping("/recipes/ingredients/quick-add")
    @ResponseBody
    public ResponseEntity<?> quickAddIngredient(@RequestParam("name") String ingredientName,
            @RequestParam("groceryCategoryId") Integer groceryCategoryId,
            @RequestParam(value = "classification", defaultValue = "SOLID") String classificationStr, // ◄ NEW
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

}
