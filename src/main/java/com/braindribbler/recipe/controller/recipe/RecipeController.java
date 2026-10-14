package com.braindribbler.recipe.controller.recipe;

// Core Framework Web Annotations & Utilities
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

// Core Java Collections 
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Your Local Domain Models & Service Layers
import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.domain.recipe.Recipe;
import com.braindribbler.recipe.domain.recipe.Ingredient; // ◄ FIXES missing Ingredient symbols
import com.braindribbler.recipe.dto.recipe.RecipeForm;
import com.braindribbler.recipe.service.recipe.RecipeService;
import com.braindribbler.recipe.service.recipe.IngredientService;
import com.braindribbler.recipe.security.RecipeUserDetails;

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

    @GetMapping("/recipes/my-list")
    public String showMyRecipesList(Model model, @AuthenticationPrincipal RecipeUserDetails currentUser) {
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Safely extract your core database User object using your verified profile
        // wrapper hook
        var domainUser = currentUser.getUserProfile();

        // Fetch user-owned recipe entities through the service wrapper layer
        List<Recipe> myRecipes = recipeService.getRecipesByUserId(domainUser.getUserId());
        model.addAttribute("recipesList", myRecipes);

        return "recipes/my-list";
    }

    // Add this mapping inside your active RecipeController.java

    @GetMapping("/recipes/view/{id}")
    public String viewRecipeDetail(@PathVariable("id") java.util.UUID publicId, Model model) {
        // Fetch the recipe entity layer complete with cascading steps
        Recipe recipe = recipeService.getRecipeByPublicId(publicId);
        model.addAttribute("recipe", recipe);

        return "recipes/view";
    }

    @PostMapping("/recipes/ingredients/quick-add")
    @ResponseBody
    public ResponseEntity<?> quickAddIngredient(@RequestParam("name") String ingredientName,
            @AuthenticationPrincipal RecipeUserDetails currentUser) {
        try {
            if (ingredientName == null || ingredientName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Name cannot be empty."));
            }

            // Check if the ingredient already exists to prevent unique constraint database
            // crashes
            var existing = ingredientService.getAllIngredientsSorted().stream()
                    .filter(i -> i.getIngredientName().equalsIgnoreCase(ingredientName.trim()))
                    .findFirst();

            if (existing.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "id", existing.get().getIngredientId(),
                        "name", existing.get().getIngredientName(),
                        "message", "Ingredient already exists! Selected it for you."));
            }

            // Create new unverified ingredient entry record
            Ingredient newIng = new Ingredient();
            newIng.setIngredientName(ingredientName.trim());
            newIng.setIsVerified(false); // Flags it for your Admin verification queue panel!
            newIng.setCreatedByUser(currentUser.getUserProfile());

            // Fetch a default baseline category (ID 1) to satisfy your non-null foreign key
            // constraint
            // Replace this template with your actual lookups or a fallback row
            com.braindribbler.recipe.domain.recipe.GroceryCategory defaultCategory = new com.braindribbler.recipe.domain.recipe.GroceryCategory();
            defaultCategory.setGroceryCategoryId(1);
            newIng.setGroceryCategory(defaultCategory);

            // Save entry via repository interface wrap inside your service or map it
            // locally
            // Assuming your ingredientRepository is injected or exposed via
            // ingredientService
            // For quick execution, add a simple save method to ingredientService or call it
            // here
            Ingredient saved = ingredientService.saveRawIngredient(newIng);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "id", saved.getIngredientId(),
                    "name", saved.getIngredientName(),
                    "message", "Ingredient added locally and queued for moderation review!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
