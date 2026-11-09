package com.braindribbler.recipe.controller.recipe;

// Core Java Collections 
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.domain.recipe.IngredientClassification;
import com.braindribbler.recipe.domain.recipe.Measurement;
import com.braindribbler.recipe.domain.recipe.Recipe;
import com.braindribbler.recipe.domain.recipe.RecipeStepIngredient;
import com.braindribbler.recipe.dto.recipe.RecipeForm;
import com.braindribbler.recipe.security.RecipeUserDetails;
import com.braindribbler.recipe.service.recipe.IngredientService;
import com.braindribbler.recipe.service.recipe.MetadataLookupService;
import com.braindribbler.recipe.service.recipe.RecipeCategoryService;
import com.braindribbler.recipe.service.recipe.RecipeService;
import com.braindribbler.recipe.util.RecipeUtility;

@Controller
public class RecipeController {

    private final RecipeService recipeService;
    private final IngredientService ingredientService;
    private final RecipeCategoryService recipeCategoryService;
    private final MetadataLookupService metadataLookupService;

    public RecipeController(RecipeService recipeService, 
                            IngredientService ingredientService, 
                            RecipeCategoryService recipeCategoryService, 
                            MetadataLookupService metadataLookupService) {
        this.recipeService = recipeService;
        this.ingredientService = ingredientService;
        this.recipeCategoryService = recipeCategoryService;
        this.metadataLookupService = metadataLookupService;
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

        int freshCount = 0;
        if (currentUser != null) {
            Integer userId = currentUser.getUserProfile().getUserId();
            freshCount = (int) ingredientService.getFreshIngredientsCount(userId);
        }

        // Inject the total count variable to the view engine
        model.addAttribute("freshIngredientsCount", freshCount);
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
        if (currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("can_moderate_recipe_metadata"))) {
            if (pendingIngredients > 0) {
                dashboardAlerts.add("There are " + pendingIngredients
                        + " unverified ingredients in the master pool requiring moderator review.");
            }
        }
        model.addAttribute("dashboardAlerts", dashboardAlerts);

        return "recipes/dashboard";
    }
    @GetMapping("/recipes/create")
    public String showCreateForm(Model model, @AuthenticationPrincipal RecipeUserDetails currentUser) {
        model.addAttribute("recipeForm", new RecipeForm());
        model.addAttribute("isEditMode", false);

        // Populates lookups cleanly from RAM cache only during form generations
        populateFormMetadataDirectly(model, currentUser);

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

        Recipe savedRecipe = recipeService.saveOrUpdateRecipe(recipeForm, currentUser.getUserProfile());

        jsonResponse.put("success", true);
        jsonResponse.put("recipeId", savedRecipe.getRecipeId());

        if ("FINISH".equalsIgnoreCase(actionType)) {
            jsonResponse.put("redirectUrl", "/view/" + savedRecipe.getRecipeId());
        } else {
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
        List<Recipe> userRecipes = recipeService.getRecipesByUserId(domainUser.getUserId());
        model.addAttribute("recipes", userRecipes);
        model.addAttribute("userDisplayName", domainUser.getDisplayName());

        return "recipes/my-list";
    }

    @PreAuthorize("hasAuthority('can_hide_any_recipe')")
    @PostMapping("/recipes/view/{recipeId}/hide")
    public String hideRecipeByNumericId(@PathVariable Integer recipeId) {
        recipeService.forceRecipePrivateBySequenceId(recipeId);
        return "redirect:/view/" + recipeId;
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
            recipeForm.setRecipeId(numericRecipeId); 

            model.addAttribute("recipeForm", recipeForm);
            model.addAttribute("isEditMode", true);

            // Populates metadata values efficiently via lookups cache layer
            populateFormMetadataDirectly(model, currentUser);

            return "recipes/create";
        } catch (NumberFormatException e) {
            return "redirect:/recipes/my-list?error=invalidRecipeId";
        }
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

            if (!recipe.getUser().getUserId().equals(currentUser.getUserProfile().getUserId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized action.");
                return "redirect:/recipes/my-list";
            }

            recipeService.deleteRecipeById(numericRecipeId);
            redirectAttributes.addFlashAttribute("successMessage", "Recipe deleted successfully.");
            return "redirect:/recipes/my-list";

        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid recipe ID reference.");
            return "redirect:/recipes/my-list";
        }
    }

    @GetMapping("/view/{id}")
    public String viewPublicRecipe(@PathVariable("id") Integer id, Model model, @AuthenticationPrincipal RecipeUserDetails currentUser) {
        try {
            Recipe recipe = recipeService.getRecipeById(id);
            if (recipe == null) return "redirect:/search?notFound";

            return populateAndRenderProfileView(recipe, model, currentUser);
        } catch (IllegalArgumentException e) {
            return "redirect:/search?notFound";
        }
    }

    private String populateAndRenderProfileView(Recipe recipe, Model model, RecipeUserDetails currentUser) {
        User userProfile = (currentUser != null) ? currentUser.getUserProfile() : null;
        
        boolean canEdit = (userProfile != null) && recipe.getUser().getUserId().equals(userProfile.getUserId());
        boolean hasHidePermission = (currentUser != null) && currentUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("can_hide_any_recipe"));

        int visibilityId = recipe.getRecipeVisibility().getRecipeVisibilityId();

        if (visibilityId == 2 && !canEdit && !hasHidePermission) return "redirect:/search?unauthorized";
        if (visibilityId == 3 && !canEdit) return "redirect:/search?unauthorized";

        // Pulls exclusively from your optimized MetadataLookupService cache map abstraction
        List<Measurement> measurementsPool = metadataLookupService.getAllMeasurements();
        List<Recipe> singleRecipeSelection = Collections.singletonList(recipe);
        
        // Segregated logic blocks for uncombined display card views vs combined checkout sheets
        Map<String, List<RecipeStepIngredient>> lineItemIngredients = RecipeUtility
                .groupIngredientsByGroceryCategory(singleRecipeSelection);
        Map<String, List<RecipeStepIngredient>> combinedShoppingList = RecipeUtility
                .aggregateAndPromoteShoppingList(singleRecipeSelection, measurementsPool);

        model.addAttribute("recipe", recipe);
        model.addAttribute("canEdit", canEdit);
        model.addAttribute("isOwner", canEdit);
        model.addAttribute("canHide", hasHidePermission && visibilityId != 2);
        
        model.addAttribute("singleRecipeAggregatedIngredients", lineItemIngredients);
        model.addAttribute("shoppingListAggregatedIngredients", combinedShoppingList);

        return "recipes/view";
    }

    private void populateFormMetadataDirectly(Model model, RecipeUserDetails currentUser) {
        User userEntity = (currentUser != null) ? currentUser.getUserProfile() : null;

        model.addAttribute("recipeCategories", recipeCategoryService.getAllCategories());
        model.addAttribute("allIngredients", ingredientService.getAllIngredientsSorted(userEntity));
        model.addAttribute("measurements", metadataLookupService.getAllMeasurements());
        model.addAttribute("visibilities", metadataLookupService.getAllVisibilities());
        model.addAttribute("groceryCategories", ingredientService.getAllGroceryCategoriesSortedByOrder());
        model.addAttribute("classifications", IngredientClassification.values());
    }
}
