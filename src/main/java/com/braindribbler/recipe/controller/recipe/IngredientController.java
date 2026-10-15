package com.braindribbler.recipe.controller.recipe;

import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.domain.recipe.Ingredient;
import com.braindribbler.recipe.security.RecipeUserDetails;
import com.braindribbler.recipe.service.recipe.IngredientService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/recipes/ingredients")
public class IngredientController {

    private final IngredientService ingredientService;

    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    /**
     * Handles inline AJAX quick-adds for missing ingredients during recipe
     * construction.
     * Maps to the path: /recipes/ingredients/quick-add
     */
    @PostMapping("/quick-add")
    public ResponseEntity<?> quickAddIngredient(@RequestParam("name") String ingredientName,
            @RequestParam("groceryCategoryId") Integer groceryCategoryId,
            @AuthenticationPrincipal RecipeUserDetails currentUser) {
        try {
            if (currentUser == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("success", false, "message", "Session expired. Please log in again."));
            }
            if (ingredientName == null || ingredientName.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Ingredient name cannot be empty."));
            }

            User userEntity = currentUser.getUserProfile();

            // This service method automatically validates case-insensitive duplication
            // and handles the full transactional persist mapping sequence cleanly!
            Ingredient saved = ingredientService.quickRegisterIngredient(ingredientName, groceryCategoryId, userEntity);

            // Returns the exact JSON key shapes expected by your create.html jQuery script
            // block
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "ingredientId", saved.getIngredientId(),
                    "ingredientName", saved.getIngredientName(),
                    "message", "Ingredient added and queued for moderation review!"));
        } catch (IllegalArgumentException e) {
            // Catches our case-insensitive duplication exception cleanly and passes the
            // message to the modal feedback banner
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "An unexpected system error occurred."));
        }
    }
}
