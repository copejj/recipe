package com.braindribbler.recipe.controller.recipe;

// Core Java Collections 
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

// Your Local Domain Models & Service Layers
import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.domain.recipe.Measurement;
import com.braindribbler.recipe.domain.recipe.Recipe;
import com.braindribbler.recipe.domain.recipe.RecipeStepIngredient;
import com.braindribbler.recipe.dto.recipe.RecipeForm;
import com.braindribbler.recipe.security.RecipeUserDetails;
import com.braindribbler.recipe.service.recipe.IngredientService;
import com.braindribbler.recipe.service.recipe.RecipeService;
import com.braindribbler.recipe.util.RecipeUtility;

import jakarta.servlet.http.HttpServletRequest;

// ... Your active RecipeController class implementation body continues below seamlessly ...

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

        // 1. Gather baseline counter metrics via services
        long myRecipesCount = recipeService.getRecipeCountByUserId(domainUser.getUserId());
        int pendingIngredients = ingredientService.getUnverifiedIngredients().size();

        model.addAttribute("myRecipesCount", myRecipesCount);
        model.addAttribute("pendingIngredientsCount", pendingIngredients);
        model.addAttribute("totalRecipesCount", recipeService.getTotalRecipesCount());

        // 2. Fetch message cleanly via service layer wrapper ---
        ingredientService.getLatestActiveMessageForUser(domainUser.getUserId())
                .ifPresent(msg -> {
                    model.addAttribute("dbMessageText", msg.getMessageText());
                    model.addAttribute("dbMessageClass", "alert--" + msg.getAlertType());
                });

        // 3. Dynamic Moderator Action Alerts
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
    public String showCreateForm(Model model, @AuthenticationPrincipal RecipeUserDetails currentUser) {

        model.addAttribute("recipeForm", new RecipeForm());
        model.addAttribute("isEditMode", false);

        return "recipes/create";
    }

    @PostMapping("/recipes/create")
    public String handleRecipeSubmission(@ModelAttribute("recipeForm") RecipeForm recipeForm,
            @AuthenticationPrincipal RecipeUserDetails currentUser) {
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Call our unified single transaction handler method
        Recipe savedRecipe = recipeService.saveOrUpdateRecipe(recipeForm, currentUser.getUserProfile());

        // Redirect the user straight to their gorgeous newly updated recipe viewing
        // sheet profile
        return "redirect:/recipes/view/" + savedRecipe.getRecipeId();
    }

    @GetMapping("/recipes/my-list")
    public String showMyRecipesList(Model model, @AuthenticationPrincipal RecipeUserDetails currentUser) {
        if (currentUser == null) {
            return "redirect:/login";
        }

        var domainUser = currentUser.getUserProfile();

        // Fetch the scoped list of recipes owned explicitly by this chef account
        List<Recipe> userRecipes = recipeService.getRecipesByUserId(domainUser.getUserId());
        model.addAttribute("recipes", userRecipes);
        model.addAttribute("userDisplayName", domainUser.getDisplayName());

        return "recipes/my-list";
    }

    @GetMapping("/recipes/view/{id}")
    public String viewRecipeProfile(@PathVariable("id") Integer id, Model model) {
        // 1. Fetch your recipe record entity from the database repository layer
        Recipe recipe = recipeService.getRecipeById(id);

        // 2. CRITICAL LOOKUP: Fetch all master measurements from your database pool
        // This gives your auto-promoter access to scaling_ranks and unit equivalents!
        List<Measurement> measurementsPool = recipeService.getAllMeasurements();

        // 3. Wrap your single recipe inside a list wrapper container
        List<Recipe> singleRecipeSelection = Collections.singletonList(recipe);

        // 4. Run your type-safe BigDecimal scaling and category-sorting math engine
        Map<String, List<RecipeStepIngredient>> structuredIngredients = RecipeUtility
                .aggregateAndPromoteShoppingList(singleRecipeSelection, measurementsPool);

        // 5. Inject all matching data attributes down to the Thymeleaf model view
        // context
        model.addAttribute("recipe", recipe);

        // FIXED: Passes data explicitly to your Overview summary panel loop variable
        model.addAttribute("singleRecipeAggregatedIngredients", structuredIngredients);

        // FIXED: Passes data explicitly to your Market Shopping panel loop variable
        model.addAttribute("shoppingListAggregatedIngredients", structuredIngredients);

        return "recipes/view";
    }

    @GetMapping("/recipes/edit/{id}")
    public String showEditRecipeForm(@PathVariable("id") String id, Model model,
            @AuthenticationPrincipal RecipeUserDetails currentUser) {
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            Integer numericRecipeId = Integer.parseInt(id.trim());
            Recipe recipe = recipeService.getRecipeById(numericRecipeId);

            if (!recipe.getUser().getUserId().equals(currentUser.getUserProfile().getUserId())) {
                return "redirect:/recipes/my-list?unauthorized";
            }

            RecipeForm recipeForm = recipeService.prepareFormFromRecipe(recipe);
            recipeForm.setRecipeId(numericRecipeId); // ◄ CRITICAL: Attaches the active row ID to the form!

            model.addAttribute("recipeForm", recipeForm);
            model.addAttribute("isEditMode", true);

            return "recipes/create";
        } catch (NumberFormatException e) {
            return "redirect:/recipes/my-list?error=invalidRecipeId";
        }
    }

    @ModelAttribute
    public void populateFormMetadata(Model model,
            @AuthenticationPrincipal RecipeUserDetails currentUser,
            HttpServletRequest request) {

        String method = request.getMethod();

        // CRITICAL EFFICIENCY GUARD: Only query the database for GET requests (page
        // renders)
        // Completely skips queries on POST submissions, deletes, or updates!
        if (!"GET".equalsIgnoreCase(method)) {
            return;
        }

        User userEntity = (currentUser != null) ? currentUser.getUserProfile() : null;

        model.addAttribute("allIngredients", ingredientService.getAllIngredientsSorted(userEntity));
        model.addAttribute("measurements", recipeService.getAllMeasurements());
        model.addAttribute("visibilities", recipeService.getAllVisibilities());
        model.addAttribute("groceryCategories", ingredientService.getAllGroceryCategories());
    }
}
