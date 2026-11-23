package com.braindribbler.recipe.controller.mealplan;

import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.domain.mealplan.ShoppingList;
import com.braindribbler.recipe.domain.recipe.GroceryCategory;
import com.braindribbler.recipe.domain.recipe.Ingredient;
import com.braindribbler.recipe.domain.recipe.Measurement;
import com.braindribbler.recipe.repository.recipe.GroceryCategoryRepository;
import com.braindribbler.recipe.repository.recipe.IngredientRepository;
import com.braindribbler.recipe.repository.recipe.MeasurementRepository;
import com.braindribbler.recipe.security.RecipeUserDetails;
import com.braindribbler.recipe.service.mealplan.MealPlanService;
import com.braindribbler.recipe.service.mealplan.ShoppingListService;
import com.braindribbler.recipe.service.recipe.IngredientService;
import com.braindribbler.recipe.service.recipe.MetadataLookupService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/shopping-lists")
public class ShoppingListController {

    private final ShoppingListService shoppingListService;
    private final MealPlanService mealPlanService;
    private final IngredientService ingredientService;
    private final MetadataLookupService metadataLookupService;
    private final IngredientRepository ingredientRepository;
    private final MeasurementRepository measurementRepository;
    private final GroceryCategoryRepository groceryCategoryRepository;

    public ShoppingListController(ShoppingListService shoppingListService,
                                  MealPlanService mealPlanService,
                                  IngredientService ingredientService,
                                  MetadataLookupService metadataLookupService,
                                  IngredientRepository ingredientRepository,
                                  MeasurementRepository measurementRepository,
                                  GroceryCategoryRepository groceryCategoryRepository) {
        this.shoppingListService = shoppingListService;
        this.mealPlanService = mealPlanService;
        this.ingredientService = ingredientService;
        this.metadataLookupService = metadataLookupService;
        this.ingredientRepository = ingredientRepository;
        this.measurementRepository = measurementRepository;
        this.groceryCategoryRepository = groceryCategoryRepository;
    }

    @GetMapping("/{publicId}")
    public String viewShoppingList(@PathVariable UUID publicId,
                                   Model model,
                                   @AuthenticationPrincipal RecipeUserDetails currentUser) {
        if (currentUser == null) return "redirect:/login";
        User user = currentUser.getUserProfile();
        ShoppingList shoppingList = shoppingListService.getByPublicId(publicId);

        boolean canAccess = shoppingList.getOwner().getUserId().equals(user.getUserId())
                || (shoppingList.getMealPlan() != null && mealPlanService.canUserView(shoppingList.getMealPlan(), user));

        if (!canAccess) {
            return "redirect:/meal-plans?unauthorized";
        }

        model.addAttribute("shoppingList", shoppingList);
        model.addAttribute("itemsByAisle", shoppingListService.groupItemsByAisle(shoppingList));
        model.addAttribute("groceryCategories", ingredientService.getAllGroceryCategoriesSortedByOrder());
        model.addAttribute("measurements", metadataLookupService.getAllMeasurements());
        model.addAttribute("allIngredients", ingredientService.getAllIngredientsSorted(user));

        return "shopping-lists/view";
    }

    @PostMapping("/{publicId}/items/add")
    public String addExtraStoreItem(@PathVariable UUID publicId,
                                    @RequestParam(required = false) Integer ingredientId,
                                    @RequestParam(required = false) String customItemName,
                                    @RequestParam(required = false) Integer groceryCategoryId,
                                    @RequestParam(required = false) BigDecimal amount,
                                    @RequestParam(required = false) Integer measurementId,
                                    @RequestParam(required = false) String note,
                                    @AuthenticationPrincipal RecipeUserDetails currentUser,
                                    RedirectAttributes redirectAttributes) {
        if (currentUser == null) return "redirect:/login";
        ShoppingList list = shoppingListService.getByPublicId(publicId);

        if (ingredientId == null && (customItemName == null || customItemName.isBlank())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please enter an item name.");
            return "redirect:/shopping-lists/" + publicId;
        }

        Ingredient ingredient = (ingredientId != null) ? ingredientRepository.findById(ingredientId).orElse(null) : null;
        GroceryCategory category = (groceryCategoryId != null) ? groceryCategoryRepository.findById(groceryCategoryId).orElse(null) : null;
        Measurement unit = (measurementId != null) ? measurementRepository.findById(measurementId).orElse(null) : null;

        shoppingListService.addExtraItem(list, ingredient, customItemName, category, amount, unit, note, currentUser.getUserProfile());
        redirectAttributes.addFlashAttribute("successMessage", "Added item to shopping list.");
        return "redirect:/shopping-lists/" + publicId;
    }

    @PostMapping("/{publicId}/items/{itemId}/toggle")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleItem(@PathVariable UUID publicId,
                                                          @PathVariable Integer itemId,
                                                          @AuthenticationPrincipal RecipeUserDetails currentUser) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        boolean isChecked = shoppingListService.toggleItemChecked(itemId);
        return ResponseEntity.ok(Map.of("itemId", itemId, "isChecked", isChecked));
    }

    @PostMapping("/{publicId}/items/{itemId}/delete")
    public String deleteItem(@PathVariable UUID publicId,
                             @PathVariable Integer itemId,
                             @AuthenticationPrincipal RecipeUserDetails currentUser) {
        if (currentUser == null) return "redirect:/login";
        shoppingListService.deleteItem(itemId);
        return "redirect:/shopping-lists/" + publicId;
    }

    @PostMapping("/{publicId}/reset")
    public String resetShoppingList(@PathVariable UUID publicId,
                                    @AuthenticationPrincipal RecipeUserDetails currentUser,
                                    RedirectAttributes redirectAttributes) {
        if (currentUser == null) return "redirect:/login";
        User user = currentUser.getUserProfile();
        ShoppingList list = shoppingListService.getByPublicId(publicId);

        boolean canAccess = list.getOwner().getUserId().equals(user.getUserId())
                || (list.getMealPlan() != null && mealPlanService.canUserView(list.getMealPlan(), user));

        if (!canAccess) {
            return "redirect:/meal-plans?unauthorized";
        }

        shoppingListService.resetFromMealPlan(list, user);
        redirectAttributes.addFlashAttribute("successMessage", "Shopping list reset from meal plan!");
        return "redirect:/shopping-lists/" + publicId;
    }

    @GetMapping("/{publicId}/state")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getShoppingListState(@PathVariable UUID publicId,
                                                                    @AuthenticationPrincipal RecipeUserDetails currentUser) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        User user = currentUser.getUserProfile();
        ShoppingList list = shoppingListService.getByPublicId(publicId);

        boolean canAccess = list.getOwner().getUserId().equals(user.getUserId())
                || (list.getMealPlan() != null && mealPlanService.canUserView(list.getMealPlan(), user));

        if (!canAccess) {
            return ResponseEntity.status(403).build();
        }

        List<Map<String, Object>> itemsState = list.getItems().stream()
                .map(i -> Map.<String, Object>of(
                        "id", i.getShoppingListItemId(),
                        "checked", Boolean.TRUE.equals(i.getIsChecked())
                ))
                .toList();

        return ResponseEntity.ok(Map.of(
                "count", itemsState.size(),
                "items", itemsState
        ));
    }
}
