package com.braindribbler.recipe.controller.recipe;

import com.braindribbler.recipe.domain.recipe.Recipe;
import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.dto.recipe.RecipeForm;
import com.braindribbler.recipe.service.recipe.RecipeService;
import com.braindribbler.recipe.service.recipe.IngredientService;
import com.braindribbler.recipe.security.RecipeUserDetails;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.ArrayList;
import java.util.List;

@Controller
public class RecipeController {

    private final RecipeService recipeService;
    private final IngredientService ingredientService;

    public RecipeController(RecipeService recipeService, IngredientService ingredientService) {
        this.recipeService = recipeService;
        this.ingredientService = ingredientService;
    }

    @GetMapping("/")
    public String getDashboard(Model model) {
        Recipe sampleRecipe = new Recipe();
        sampleRecipe.setTitle("Chocolate Chip Cookies");
        sampleRecipe.setDescription("Mix, bake, and enjoy!");
        model.addAttribute("recipe", sampleRecipe);
        return "index";
    }

    @GetMapping("/dashboard")
    public String showHome(Model model, @AuthenticationPrincipal RecipeUserDetails currentUser) {
        if (currentUser == null) {
            return "redirect:/login";
        }

        User domainUser = currentUser.getUserProfile();
        model.addAttribute("userDisplayName", domainUser.getDisplayName());

        // Gather real-time workspace metric counters
        long myRecipesCount = recipeService.getRecipeCountByUserId(domainUser.getUserId());
        int pendingIngredients = ingredientService.getUnverifiedIngredients().size();

        model.addAttribute("myRecipesCount", myRecipesCount);
        model.addAttribute("pendingIngredientsCount", pendingIngredients);
        model.addAttribute("totalRecipesCount", recipeService.getTotalRecipesCount());

        // --- NEW: Dynamic Time-of-Day Call to Action Greetings ---
        int hour = java.time.LocalTime.now().getHour();
        String timeGreeting;
        if (hour < 12) {
            timeGreeting = "Good morning";
        } else if (hour < 17) {
            timeGreeting = "Good afternoon";
        } else {
            timeGreeting = "Good evening";
        }

        // Create an inspirational subtitle tailored directly to the workspace context
        String customizedSubtitle = String.format("%s, %s! What are we cooking up in the kitchen today?",
                timeGreeting, domainUser.getDisplayName());
        model.addAttribute("message", customizedSubtitle);

        // Dynamic Dashboard Action Alerts Engine
        List<String> dashboardAlerts = new ArrayList<>();
        if (currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("can_moderate_ingredients"))) {
            if (pendingIngredients > 0) {
                dashboardAlerts.add("There are " + pendingIngredients
                        + " unverified ingredients in the master pool requiring moderator review.");
            }
        }

        model.addAttribute("dashboardAlerts", dashboardAlerts);
        return "recipes/dashboard";
    }

    @GetMapping("/search")
    public String showSearch(Model model) {
        model.addAttribute("message", "Search for your favorite recipes here!");
        return "recipes/search";
    }

    @GetMapping("/recipes/create")
    public String showCreateForm(Model model) {
        model.addAttribute("recipeForm", new RecipeForm());
        populateFormMetadata(model);
        return "recipes/create";
    }

    // --- FIXED: Enabled secure multi-table saving linked to your actual domain
    // User profile ---
    @PostMapping("/recipes/create")
    public String handleRecipeCreation(@ModelAttribute("recipeForm") RecipeForm recipeForm,
            @AuthenticationPrincipal RecipeUserDetails currentUser) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        recipeService.saveRecipeFromForm(recipeForm, currentUser.getUserProfile());
        return "redirect:/dashboard";
    }

    private void populateFormMetadata(Model model) {
        model.addAttribute("allIngredients", ingredientService.getAllIngredientsSorted());
        model.addAttribute("measurements", recipeService.getAllMeasurements());
        model.addAttribute("visibilities", recipeService.getAllVisibilities());
    }
}
