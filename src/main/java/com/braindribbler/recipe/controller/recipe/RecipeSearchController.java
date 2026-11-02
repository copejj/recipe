package com.braindribbler.recipe.controller.recipe;

import com.braindribbler.recipe.domain.recipe.Recipe;
import com.braindribbler.recipe.domain.recipe.RecipeStepIngredient;
import com.braindribbler.recipe.domain.recipe.Ingredient;
import com.braindribbler.recipe.service.recipe.RecipeCategoryService;
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
    private final RecipeCategoryService recipeCategoryService; 

    public RecipeSearchController(RecipeService recipeService, RecipeCategoryService recipeCategoryService) {
        this.recipeService = recipeService;
        this.recipeCategoryService = recipeCategoryService;
    }

    /**
     * Unified search dashboard endpoint.
     * Displays a single table layout filtered dynamically by chosen ingredients or categories.
     * Route: GET /search
     */
    @GetMapping("/search")
    public String unifiedSearchConsole(
            @RequestParam(value = "ingredientIds", required = false) List<Integer> ingredientIds,
            @RequestParam(value = "recipeCategoryId", required = false) Integer recipeCategoryId, // ◄ ADD THIS
            Model model,
            @AuthenticationPrincipal RecipeUserDetails currentUser) {

        User userProfile = (currentUser != null) ? currentUser.getUserProfile() : null; //

        // 1. Always supply master metadata selection pools on every view load
        model.addAttribute("ingredientPool", recipeService.getAllIngredientsAlphabetical()); //
        model.addAttribute("categoryPool", recipeCategoryService.getAllCategories()); // ◄ ADD THIS

        List<Recipe> tableRowsPool;
        boolean isFilteredView = false; //

        // 2. Load matching recipe subset if ingredient parameters exist
        if (ingredientIds != null && !ingredientIds.isEmpty()) { //
            tableRowsPool = recipeService.searchByIngredientsOnHand(ingredientIds, userProfile); //
            isFilteredView = true; //
            model.addAttribute("activeFilterIds", ingredientIds); //
        } 
        // 3. NEW: Fallback filter by selected Recipe Category
        else if (recipeCategoryId != null) {
            tableRowsPool = recipeService.getRecipesByCategoryId(recipeCategoryId);
            isFilteredView = true;
            model.addAttribute("selectedCategoryId", recipeCategoryId);
        }
        // 4. Fall back to loading the comprehensive catalog index generally
        else {
            tableRowsPool = recipeService.getGeneralBrowsingPool(userProfile); //
        }

        model.addAttribute("recipesIndexPool", tableRowsPool); //
        model.addAttribute("isFilteredView", isFilteredView); //

        return "recipes/search/dashboard"; //
    }

    // Handles: /view/4de5677f-3370-4ff9-af60-8f9c44fc4d88
    @PreAuthorize("hasAuthority('can_hide_any_recipe')")
    @PostMapping("/view/{publicId}/hide")
    public String hideRecipeByPublicUuid(@PathVariable UUID publicId) {
        recipeService.forceRecipePrivateByPublicUuid(publicId);
        return "redirect:/view/" + publicId;
    }
}
