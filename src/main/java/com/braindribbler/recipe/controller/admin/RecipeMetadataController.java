package com.braindribbler.recipe.controller.admin;

import com.braindribbler.recipe.service.recipe.IngredientService;
import com.braindribbler.recipe.service.recipe.RecipeCategoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/recipes/metadata")
@PreAuthorize("hasAuthority('can_moderate_recipe_metadata')")
public class RecipeMetadataController {

    private final IngredientService ingredientService;
    private final RecipeCategoryService recipeCategoryService;

    public RecipeMetadataController(IngredientService ingredientService, RecipeCategoryService recipeCategoryService) {
        this.ingredientService = ingredientService;
        this.recipeCategoryService = recipeCategoryService;
    }

    /**
     * Renders the high-level Unified Executive Metrics Control Dashboard Center
     */
    @GetMapping
    public String showExecutiveDashboard(Model model) {
        int pendingItems = ingredientService.getUnverifiedIngredients().size();
        int verifiedItems = ingredientService.getAllIngredientsSorted(null).size();
        int groceryAisles = ingredientService.getAllGroceryCategories().size();
        int recipeCategoriesCount = recipeCategoryService.getAllCategories().size();

        model.addAttribute("pendingCount", pendingItems);
        model.addAttribute("totalCount", verifiedItems);
        model.addAttribute("categoryCount", groceryAisles);
        model.addAttribute("recipeCategoriesCount", recipeCategoriesCount);

        return "admin/recipes/metadata/dashboard";
    }
}
