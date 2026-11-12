package com.braindribbler.recipe.controller.recipe;

import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.dto.recipe.RecipeSaveBreakdown;
import com.braindribbler.recipe.dto.recipe.RecipeStats;
import com.braindribbler.recipe.dto.recipe.SavedRecipeSummary;
import com.braindribbler.recipe.repository.auth.UserRepository;
import com.braindribbler.recipe.repository.recipe.RecipeRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/recipes")
public class RecipeStatsController {

    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;

    public RecipeStatsController(UserRepository userRepository, RecipeRepository recipeRepository) {
        this.userRepository = userRepository;
        this.recipeRepository = recipeRepository;
    }

    @GetMapping("/my-stats")
    public String showMyStats(@AuthenticationPrincipal UserDetails loggedInUser, Model model) {
        User currentUser = userRepository.findByUserAuthEmail(loggedInUser.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        RecipeStats recipeStats = recipeRepository.getRecipeStatsByUserId(currentUser.getUserId());
        List<RecipeSaveBreakdown> recipeBreakdown = recipeRepository.getRecipeSaveBreakdownByUserId(currentUser.getUserId());
        List<SavedRecipeSummary> savedRecipes = recipeRepository.findSavedRecipesByUserId(currentUser.getUserId());

        // Sum total views and shares across all of the user's created recipes
        long totalViews = recipeBreakdown.stream().mapToLong(RecipeSaveBreakdown::getViewCount).sum();
        long totalSharedViews = recipeBreakdown.stream().mapToLong(RecipeSaveBreakdown::getSharedViewCount).sum();
        long totalShares = recipeBreakdown.stream().mapToLong(RecipeSaveBreakdown::getShareCount).sum();

        if (recipeStats != null) {
            recipeStats.setTotalViews(totalViews);
            recipeStats.setTotalSharedViews(totalSharedViews);
            recipeStats.setTotalShares(totalShares);
        }


        model.addAttribute("currentUser", currentUser);
        model.addAttribute("recipeStats", recipeStats);
        model.addAttribute("recipeBreakdown", recipeBreakdown);
        model.addAttribute("savedRecipes", savedRecipes);

        return "recipes/my-stats";
    }
}
