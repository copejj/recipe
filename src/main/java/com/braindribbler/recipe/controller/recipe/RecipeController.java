package com.braindribbler.recipe.controller.recipe;

// Core Java Collections 
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Your Local Domain Models & Service Layers
import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.domain.recipe.IngredientClassification;
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
    @ResponseBody
    public ResponseEntity<?> handleRecipeSubmission(
            @ModelAttribute("recipeForm") RecipeForm recipeForm,
            @RequestParam(value = "actionType", defaultValue = "SAVE") String actionType,
            @AuthenticationPrincipal RecipeUserDetails currentUser) {

        Map<String, Object> jsonResponse = new HashMap<>();

        if (currentUser == null) {
            jsonResponse.put("success", false);
            jsonResponse.put("redirectUrl", "/login");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonResponse);
        }

        // 1. Call your existing transaction business handler logic
        Recipe savedRecipe = recipeService.saveOrUpdateRecipe(recipeForm, currentUser.getUserProfile());

        jsonResponse.put("success", true);
        jsonResponse.put("recipeId", savedRecipe.getRecipeId());

        // 2. Evaluate button pathing commands dynamically
        if ("FINISH".equalsIgnoreCase(actionType)) {
            // Provide the routing path for the "Finish & View Recipe" button
            jsonResponse.put("redirectUrl", "/recipes/view/" + savedRecipe.getRecipeId());
        } else {
            // Provide a stay-put confirmation path for the "Save Progress" button
            jsonResponse.put("redirectUrl", null);
        }

        return ResponseEntity.ok(jsonResponse);
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
    public String viewRecipeProfile(@PathVariable("id") Integer id,
            Model model,
            @AuthenticationPrincipal RecipeUserDetails currentUser) { // ◄ Added authentication principal

        // 1. Fetch your recipe record entity from the database repository layer
        Recipe recipe = recipeService.getRecipeById(id);

        // 2. CRITICAL LOOKUP: Fetch all master measurements from your database pool
        List<Measurement> measurementsPool = recipeService.getAllMeasurements();

        // 3. Wrap your single recipe inside a list wrapper container
        List<Recipe> singleRecipeSelection = Collections.singletonList(recipe);

        // 4. Run your type-safe BigDecimal scaling and category-sorting math engine
        Map<String, List<RecipeStepIngredient>> structuredIngredients = RecipeUtility
                .aggregateAndPromoteShoppingList(singleRecipeSelection, measurementsPool);

        // 5. DETERMINE PERMISSIONS: Evaluate edit rights cleanly using objects
        boolean canEdit = false;
        if (currentUser != null && recipe.getUser() != null) {
            // Safe, type-safe comparison of numeric IDs (Integer / Long)
            canEdit = recipe.getUser().getUserId().equals(currentUser.getUserProfile().getUserId());
        }

        // 6. Inject all matching data attributes down to the Thymeleaf model view
        // context
        model.addAttribute("recipe", recipe);
        model.addAttribute("canEdit", canEdit); // ◄ Added simple boolean flag
        model.addAttribute("singleRecipeAggregatedIngredients", structuredIngredients);
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
        if (!"GET".equalsIgnoreCase(method)) {
            return;
        }

        User userEntity = (currentUser != null) ? currentUser.getUserProfile() : null;

        model.addAttribute("allIngredients", ingredientService.getAllIngredientsSorted(userEntity));
        model.addAttribute("measurements", recipeService.getAllMeasurements());
        model.addAttribute("visibilities", recipeService.getAllVisibilities());

        model.addAttribute("groceryCategories", ingredientService.getAllGroceryCategoriesSortedByOrder());
        model.addAttribute("classifications", IngredientClassification.values());
    }

    @PostMapping("/recipes/delete/{id}")
    public String deleteRecipe(@PathVariable("id") String id,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal RecipeUserDetails currentUser) {
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            Integer numericRecipeId = Integer.parseInt(id.trim());
            Recipe recipe = recipeService.getRecipeById(numericRecipeId);

            // Security Check: Ensure the logged-in user actually owns this recipe
            if (!recipe.getUser().getUserId().equals(currentUser.getUserProfile().getUserId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized action.");
                return "redirect:/recipes/my-list";
            }

            // Execute service deletion cascading
            recipeService.deleteRecipeById(numericRecipeId);

            // Use flash attributes to show a sweet alert or toast on the listing page
            redirectAttributes.addFlashAttribute("successMessage", "Recipe deleted successfully.");
            return "redirect:/recipes/my-list";

        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid recipe ID reference.");
            return "redirect:/recipes/my-list";
        }
    }
}
