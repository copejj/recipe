package com.braindribbler.recipe.controller.mealplan;

import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.domain.mealplan.MealPlan;
import com.braindribbler.recipe.domain.mealplan.MealPlanRecipe;
import com.braindribbler.recipe.domain.mealplan.ShoppingList;
import com.braindribbler.recipe.domain.recipe.AccessLevel;
import com.braindribbler.recipe.domain.recipe.Recipe;
import com.braindribbler.recipe.repository.auth.UserRepository;
import com.braindribbler.recipe.repository.recipe.AccessLevelRepository;
import com.braindribbler.recipe.security.RecipeUserDetails;
import com.braindribbler.recipe.service.mealplan.MealPlanService;
import com.braindribbler.recipe.service.mealplan.ShoppingListService;
import com.braindribbler.recipe.service.recipe.RecipeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Controller
@RequestMapping("/meal-plans")
public class MealPlanController {

    private final MealPlanService mealPlanService;
    private final ShoppingListService shoppingListService;
    private final RecipeService recipeService;
    private final UserRepository userRepository;
    private final AccessLevelRepository accessLevelRepository;

    public MealPlanController(MealPlanService mealPlanService,
                              ShoppingListService shoppingListService,
                              RecipeService recipeService,
                              UserRepository userRepository,
                              AccessLevelRepository accessLevelRepository) {
        this.mealPlanService = mealPlanService;
        this.shoppingListService = shoppingListService;
        this.recipeService = recipeService;
        this.userRepository = userRepository;
        this.accessLevelRepository = accessLevelRepository;
    }

    @GetMapping
    public String listMealPlans(Model model, @AuthenticationPrincipal RecipeUserDetails currentUser) {
        if (currentUser == null) return "redirect:/login";
        User user = currentUser.getUserProfile();

        model.addAttribute("mealPlans", mealPlanService.getAccessibleMealPlans(user.getUserId()));
        model.addAttribute("currentUser", user);
        return "meal-plans/index";
    }

    @PostMapping("/create")
    public String createMealPlan(@RequestParam String planName,
                                 @RequestParam(required = false) String description,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                 @RequestParam(defaultValue = "false") boolean isRecurring,
                                 @AuthenticationPrincipal RecipeUserDetails currentUser,
                                 RedirectAttributes redirectAttributes) {
        if (currentUser == null) return "redirect:/login";

        MealPlan created = mealPlanService.createMealPlan(
                currentUser.getUserProfile(), planName, description, startDate, isRecurring);
        redirectAttributes.addFlashAttribute("successMessage", "Meal plan created!");
        return "redirect:/meal-plans/" + created.getPublicId();
    }

        @GetMapping("/{publicId}")
    public String viewMealPlan(@PathVariable UUID publicId,
                               Model model,
                               @AuthenticationPrincipal RecipeUserDetails currentUser) {
        if (currentUser == null) return "redirect:/login";
        User user = currentUser.getUserProfile();
        MealPlan plan = mealPlanService.getByPublicId(publicId);

        if (!mealPlanService.canUserView(plan, user)) {
            return "redirect:/meal-plans?unauthorized";
        }

        Set<Integer> alreadySharedIds = plan.getShares().stream()
            .filter(s -> s.getSharedUser() != null)
            .map(s -> s.getSharedUser().getUserId())
            .collect(java.util.stream.Collectors.toSet());

        List<User> shareableUsers = userRepository.findShareableUsers(user.getUserId()).stream()
            .filter(u -> !alreadySharedIds.contains(u.getUserId()))
            .toList();

        model.addAttribute("shareableUsers", shareableUsers);


        List<MealPlanRecipe> scheduledMeals = plan.getPlannedRecipes().stream()
            .filter(e -> e.isScheduled())
            .toList();


        model.addAttribute("plan", plan);
        model.addAttribute("canEdit", mealPlanService.canUserEdit(plan, user));
        model.addAttribute("isOwner", plan.getUser().getUserId().equals(user.getUserId()));
        model.addAttribute("scheduledMeals", scheduledMeals);
        model.addAttribute("availableRecipes", recipeService.getRecipesByUserId(user.getUserId()));
        model.addAttribute("accessLevels", accessLevelRepository.findAll());

        Set<Recipe> savedCookbookRecipes = userRepository.findByIdWithSavedRecipes(user.getUserId())
            .map(u -> u.getSavedRecipes())
            .orElse(Collections.emptySet());

        model.addAttribute("savedCookbookRecipes", savedCookbookRecipes);

        return "meal-plans/view";
    }

    @PostMapping("/{publicId}/recipes/add")
    public String addRecipeToPlan(@PathVariable UUID publicId,
                                  @RequestParam Integer recipeId,
                                  @RequestParam(required = false) Short dayOfWeek,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduledDate,
                                  @RequestParam(required = false) String mealType,
                                  @RequestParam(required = false) Integer servingsOverride,
                                  @AuthenticationPrincipal RecipeUserDetails currentUser,
                                  RedirectAttributes redirectAttributes) {
        if (currentUser == null) return "redirect:/login";
        MealPlan plan = mealPlanService.getByPublicId(publicId);

        if (Boolean.TRUE.equals(plan.getIsRecurring()) && dayOfWeek == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a day of the week.");
            return "redirect:/meal-plans/" + publicId;
        }

        if (!Boolean.TRUE.equals(plan.getIsRecurring()) && scheduledDate == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a date.");
            return "redirect:/meal-plans/" + publicId;
        }

        if (!mealPlanService.canUserEdit(plan, currentUser.getUserProfile())) {
            redirectAttributes.addFlashAttribute("errorMessage", "You do not have edit access to this plan.");
            return "redirect:/meal-plans/" + publicId;
        }

        Recipe recipe = recipeService.getRecipeById(recipeId);
        mealPlanService.addRecipeToPlan(plan, recipe, dayOfWeek, scheduledDate, mealType, servingsOverride);
        shoppingListService.syncIfExistsForMealPlan(plan, currentUser.getUserProfile());

        redirectAttributes.addFlashAttribute("successMessage", "Added \"" + recipe.getTitle() + "\" to meal plan.");
        return "redirect:/meal-plans/" + publicId;

    }

    @PostMapping("/{publicId}/entries/{entryId}/schedule")
    public String scheduleEntry(@PathVariable UUID publicId,
                                @PathVariable Integer entryId,
                                @RequestParam(required = false) Short dayOfWeek,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduledDate,
                                @RequestParam(required = false) String mealType,
                                @RequestParam(required = false) Integer servingsOverride,
                                @AuthenticationPrincipal RecipeUserDetails currentUser,
                                RedirectAttributes redirectAttributes) {
        if (currentUser == null) return "redirect:/login";
        MealPlan plan = mealPlanService.getByPublicId(publicId);

        if (!mealPlanService.canUserEdit(plan, currentUser.getUserProfile())) {
            return "redirect:/meal-plans/" + publicId + "?unauthorized";
        }

        mealPlanService.scheduleEntry(entryId, dayOfWeek, scheduledDate, mealType, servingsOverride);
        redirectAttributes.addFlashAttribute("successMessage", "Schedule updated.");
        return "redirect:/meal-plans/" + publicId;
    }
    @PostMapping("/{publicId}/entries/{entryId}/remove")
    public String removeEntry(@PathVariable UUID publicId,
                              @PathVariable Integer entryId,
                              @AuthenticationPrincipal RecipeUserDetails currentUser,
                              RedirectAttributes redirectAttributes) {
        if (currentUser == null) return "redirect:/login";
        MealPlan plan = mealPlanService.getByPublicId(publicId);

        if (mealPlanService.canUserEdit(plan, currentUser.getUserProfile())) {
            mealPlanService.removeEntry(plan, entryId);
            shoppingListService.syncIfExistsForMealPlan(plan, currentUser.getUserProfile());
            redirectAttributes.addFlashAttribute("successMessage", "Removed recipe from meal plan.");
        }
        return "redirect:/meal-plans/" + publicId;
    }

    @PostMapping("/{publicId}/share")
    public String shareMealPlan(@PathVariable UUID publicId,
                                @RequestParam Integer targetUserId,
                                @RequestParam Integer accessLevelId,
                                @AuthenticationPrincipal RecipeUserDetails currentUser,
                                RedirectAttributes redirectAttributes) {
        if (currentUser == null) return "redirect:/login";
        User owner = currentUser.getUserProfile();
        MealPlan plan = mealPlanService.getByPublicId(publicId);

        if (!plan.getUser().getUserId().equals(owner.getUserId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only the owner can share this meal plan.");
            return "redirect:/meal-plans/" + publicId;
        }

        if (targetUserId.equals(owner.getUserId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "You already own this meal plan.");
            return "redirect:/meal-plans/" + publicId;
        }

        userRepository.findById(targetUserId).ifPresentOrElse(targetUser -> {
            AccessLevel level = accessLevelRepository.findById(accessLevelId).orElseThrow();
            mealPlanService.shareWithUser(plan, targetUser, level);
            redirectAttributes.addFlashAttribute("successMessage", "Shared meal plan with " + targetUser.getDisplayName() + ".");
        }, () -> redirectAttributes.addFlashAttribute("errorMessage", "Selected user not found."));

        return "redirect:/meal-plans/" + publicId;
    }

    @PostMapping("/{publicId}/shares/{shareId}/remove")
    public String revokeShare(@PathVariable UUID publicId,
                              @PathVariable Integer shareId,
                              @AuthenticationPrincipal RecipeUserDetails currentUser,
                              RedirectAttributes redirectAttributes) {
        if (currentUser == null) return "redirect:/login";
        MealPlan plan = mealPlanService.getByPublicId(publicId);

        if (!plan.getUser().getUserId().equals(currentUser.getUserProfile().getUserId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only the owner can manage sharing.");
            return "redirect:/meal-plans/" + publicId;
        }

        mealPlanService.removeShare(plan, shareId);
        redirectAttributes.addFlashAttribute("successMessage", "Removed collaborator access.");
        return "redirect:/meal-plans/" + publicId;
    }

    @PostMapping("/{publicId}/shopping-list/generate")
    public String generateShoppingList(@PathVariable UUID publicId,
                                       @RequestParam(required = false) List<Integer> selectedEntryIds,
                                       @AuthenticationPrincipal RecipeUserDetails currentUser,
                                       RedirectAttributes redirectAttributes) {
        if (currentUser == null) return "redirect:/login";
        User user = currentUser.getUserProfile();
        MealPlan plan = mealPlanService.getByPublicId(publicId);

        if (!mealPlanService.canUserView(plan, user)) {
            return "redirect:/meal-plans?unauthorized";
        }

        if (selectedEntryIds == null || selectedEntryIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select at least one meal to include in the shopping list.");
            return "redirect:/meal-plans/" + publicId;
        }

        List<MealPlanRecipe> targetEntries = plan.getPlannedRecipes().stream()
                .filter(e -> selectedEntryIds.contains(e.getMealPlanRecipeId()))
                .toList();

        ShoppingList shoppingList = shoppingListService.generateOrSyncFromMealPlan(plan, targetEntries, user);
        return "redirect:/shopping-lists/" + shoppingList.getPublicId();
    }
}
