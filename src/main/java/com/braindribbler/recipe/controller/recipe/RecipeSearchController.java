package com.braindribbler.recipe.controller.recipe;

import com.braindribbler.recipe.domain.recipe.Recipe;
import com.braindribbler.recipe.domain.recipe.RecipeStepIngredient;
import com.braindribbler.recipe.domain.recipe.Ingredient;
import com.braindribbler.recipe.service.recipe.RecipeService;
import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.security.RecipeUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

            if (recipe.getRecipeVisibility().getRecipeVisibilityId() == 3 && !isOwner) {
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
            model.addAttribute("singleRecipeAggregatedIngredients", structuredModel);
            model.addAttribute("shoppingListAggregatedIngredients", shoppingListMap); // Matches the shopping loop
                                                                                      // tokens

            return "recipes/view";

        } catch (IllegalArgumentException | jakarta.persistence.EntityNotFoundException e) {
            return "redirect:/search?notFound";
        }
    }

}
