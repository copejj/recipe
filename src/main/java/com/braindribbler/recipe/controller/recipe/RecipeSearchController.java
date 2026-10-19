package com.braindribbler.recipe.controller.recipe;

import com.braindribbler.recipe.domain.recipe.Recipe;
import com.braindribbler.recipe.domain.recipe.RecipeStepIngredient;
import com.braindribbler.recipe.domain.recipe.Ingredient;
import com.braindribbler.recipe.service.recipe.RecipeService;
import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.security.RecipeUserDetails;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@Controller
public class RecipeSearchController {

    private final RecipeService recipeService;

    public RecipeSearchController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    /**
     * Unified search dashboard endpoint.
     * Displays a single table layout filtered dynamically by chosen ingredients.
     * Route: GET /search
     */
    @GetMapping("/search")
    public String unifiedSearchConsole(
            @RequestParam(value = "ingredientIds", required = false) List<Integer> ingredientIds,
            Model model,
            @AuthenticationPrincipal RecipeUserDetails currentUser) {

        User userProfile = (currentUser != null) ? currentUser.getUserProfile() : null;

        // 1. Always supply the master selections pool on every load
        List<Ingredient> masterIngredientPool = recipeService.getAllIngredientsAlphabetical();
        model.addAttribute("ingredientPool", masterIngredientPool);

        List<Recipe> tableRowsPool;
        boolean isFilteredView = false;

        // 2. Load the matching recipe subset if ingredient parameters exist
        if (ingredientIds != null && !ingredientIds.isEmpty()) {
            tableRowsPool = recipeService.searchByIngredientsOnHand(ingredientIds, userProfile);
            isFilteredView = true;
            model.addAttribute("activeFilterIds", ingredientIds);
        }
        // 3. Fall back to loading the comprehensive catalog index generally
        else {
            tableRowsPool = recipeService.getGeneralBrowsingPool(userProfile);
        }

        model.addAttribute("recipesIndexPool", tableRowsPool);
        model.addAttribute("isFilteredView", isFilteredView);

        return "recipes/search/dashboard";
    }

    // Handles: /view/4de5677f-3370-4ff9-af60-8f9c44fc4d88
    @PreAuthorize("hasAuthority('can_hide_any_recipe')")
    @PostMapping("/view/{publicId}/hide")
    public String hideRecipeByPublicUuid(@PathVariable UUID publicId) {
        recipeService.forceRecipePrivateByPublicUuid(publicId);
        return "redirect:/view/" + publicId;
    }

    @GetMapping("/view/{publicId}")
    public String viewPublicRecipe(
            @PathVariable("publicId") String publicIdStr,
            Model model,
            @AuthenticationPrincipal RecipeUserDetails currentUser) {

        try {
            UUID publicId = UUID.fromString(publicIdStr.trim());
            Recipe recipe = recipeService.getRecipeByPublicId(publicId);

            User userProfile = (currentUser != null) ? currentUser.getUserProfile() : null;
            boolean isOwner = (userProfile != null) && recipe.getUser().getUserId().equals(userProfile.getUserId());

            // NEW: Evaluate if the current user has the moderator permission
            boolean hasHidePermission = (currentUser != null) && currentUser.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("can_hide_any_recipe"));

            int visibilityId = recipe.getRecipeVisibility().getRecipeVisibilityId();

            // NEW: Block private recipes (visibility 2) unless they are the owner or a
            // moderator
            if (visibilityId == 2 && !isOwner && !hasHidePermission) {
                return "redirect:/search?unauthorized";
            }

            // Existing logic for shared recipes (visibility 3)
            if (visibilityId == 3 && !isOwner) {
                return "redirect:/search?unauthorized";
            }

            // 1. Fetch the flat aggregated list for the overview panel checklist box
            List<RecipeStepIngredient> checklistPool = recipeService.getAggregatedIngredients(recipe);
            java.util.Map<String, List<RecipeStepIngredient>> structuredModel = new java.util.HashMap<>();
            structuredModel.put("ingredients", checklistPool);

            // 2. Fetch the category-grouped map data for the shopping list
            java.util.Map<String, List<RecipeStepIngredient>> shoppingListMap = recipeService
                    .getShoppingListIngredients(recipe);

            // 3. Inject both collection payloads into the Thymeleaf view context
            model.addAttribute("recipe", recipe);
            model.addAttribute("isOwner", isOwner);
            model.addAttribute("canEdit", isOwner); // Assuming owners can edit

            // NEW: Pass the canHide flag to the Thymeleaf view layer
            // Only true if they have the permission AND the recipe isn't already private
            model.addAttribute("canHide", hasHidePermission && visibilityId != 2);

            model.addAttribute("singleRecipeAggregatedIngredients", structuredModel);
            model.addAttribute("shoppingListAggregatedIngredients", shoppingListMap);

            return "recipes/view";

        } catch (IllegalArgumentException | jakarta.persistence.EntityNotFoundException e) {
            return "redirect:/search?notFound";
        }
    }
}
