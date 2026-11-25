package com.braindribbler.recipe.service.mealplan;

import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.domain.mealplan.*;
import com.braindribbler.recipe.domain.recipe.*;
import com.braindribbler.recipe.repository.mealplan.ShoppingListItemRepository;
import com.braindribbler.recipe.repository.mealplan.ShoppingListRepository;
import com.braindribbler.recipe.service.recipe.IngredientService;
import com.braindribbler.recipe.service.recipe.MetadataLookupService;
import com.braindribbler.recipe.util.RecipeUtility;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ShoppingListService {

    private final ShoppingListRepository shoppingListRepository;
    private final ShoppingListItemRepository shoppingListItemRepository;
    private final MetadataLookupService metadataLookupService;
    private final IngredientService ingredientService;
    private final MealPlanRecipeScaler recipeScaler;

    public ShoppingListService(ShoppingListRepository shoppingListRepository,
                               ShoppingListItemRepository shoppingListItemRepository,
                               MetadataLookupService metadataLookupService,
                               IngredientService ingredientService,
                               MealPlanRecipeScaler recipeScaler) {
        this.shoppingListRepository = shoppingListRepository;
        this.shoppingListItemRepository = shoppingListItemRepository;
        this.metadataLookupService = metadataLookupService;
        this.ingredientService = ingredientService;
        this.recipeScaler = recipeScaler;
    }

    public ShoppingList getByPublicId(UUID publicId) {
        return shoppingListRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Shopping list not found"));
    }

    @Transactional
    public ShoppingList generateOrSyncFromMealPlan(MealPlan plan, List<MealPlanRecipe> selectedEntries, User triggeredBy) {
        ShoppingList list = findOrCreateForMealPlan(plan);

        Set<Integer> checkedIngredientIds = list.getItems().stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsChecked()) && i.getIngredient() != null)
                .map(i -> i.getIngredient().getIngredientId())
                .collect(Collectors.toSet());

        Set<Integer> hiddenIngredientIds = list.getItems().stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsHidden()) && i.getIngredient() != null)
                .map(i -> i.getIngredient().getIngredientId())
                .collect(Collectors.toSet());

        // Keep manual store additions; replace recipe-aggregated rows
        list.getItems().removeIf(item -> !Boolean.TRUE.equals(item.getIsManualAddition()));

        List<Recipe> recipes = recipeScaler.prepareRecipesForAggregation(selectedEntries);
        Map<String, List<RecipeStepIngredient>> aggregated =
                RecipeUtility.aggregateAndPromoteShoppingList(recipes, metadataLookupService.getAllMeasurements());

        int order = 1;
        for (List<RecipeStepIngredient> aisleItems : aggregated.values()) {
            for (RecipeStepIngredient agg : aisleItems) {
                list.getItems().add(toPersistentItem(list, agg, triggeredBy, checkedIngredientIds, hiddenIngredientIds, order++));
            }
        }
        return shoppingListRepository.save(list);
    }

    @Transactional
    public ShoppingListItem addExtraItem(ShoppingList list, Ingredient ingredient, String customName,
                                         GroceryCategory category, BigDecimal amount, Measurement unit,
                                         String note, User addedBy) {
        ShoppingListItem item = new ShoppingListItem();
        item.setShoppingList(list);
        item.setIngredient(ingredient);
        item.setCustomItemName(ingredient == null ? customName.trim() : null);
        item.setGroceryCategory(resolveCategory(ingredient, category));
        item.setAmount(amount);
        item.setMeasurement(unit);
        item.setNote(note != null && !note.isBlank() ? note.trim() : null);
        item.setIsManualAddition(true);
        item.setAddedBy(addedBy);
        item.setSortOrder(list.getItems().size() + 1);

        list.getItems().add(item);
        return shoppingListItemRepository.save(item);
    }

    @Transactional
    public ShoppingListItem toggleItemChecked(Integer itemId) {
        ShoppingListItem item = shoppingListItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Shopping list item not found"));
        boolean nextChecked = !Boolean.TRUE.equals(item.getIsChecked());
        item.setIsChecked(nextChecked);
        if (nextChecked) {
            item.setIsQuestioned(false);
        }
        return shoppingListItemRepository.save(item);
    }

    @Transactional
    public boolean toggleItemQuestioned(Integer itemId) {
        ShoppingListItem item = shoppingListItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Shopping list item not found"));
        item.setIsQuestioned(!Boolean.TRUE.equals(item.getIsQuestioned()));
        shoppingListItemRepository.save(item);
        return item.getIsQuestioned();
    }

    @Transactional
    public boolean toggleItemHidden(Integer itemId) {
        ShoppingListItem item = shoppingListItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Shopping list item not found"));
        boolean nextHidden = !Boolean.TRUE.equals(item.getIsHidden());
        item.setIsHidden(nextHidden);
        if (nextHidden) {
            item.setIsQuestioned(false);
        }
        shoppingListItemRepository.save(item);
        return nextHidden;
    }

    @Transactional
    public void deleteItem(Integer itemId) {
        shoppingListItemRepository.deleteById(itemId);
    }

        public Map<String, List<ShoppingListItem>> groupItemsByAisle(ShoppingList list) {
        return list.getItems().stream()
                .collect(Collectors.groupingBy(item -> item.getGroceryCategory()))
                .entrySet().stream()
                .sorted(Comparator.comparingInt(e -> e.getKey().getDisplayOrder()))
                .collect(Collectors.toMap(
                        e -> e.getKey().getCategoryName(),
                        e -> e.getValue().stream()
                                .sorted(Comparator.comparing(i -> i.getDisplayName().toLowerCase()))
                                .toList(),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    private ShoppingList findOrCreateForMealPlan(MealPlan plan) {
        return shoppingListRepository.findFirstByMealPlan_MealPlanIdOrderByCreatedAtDesc(plan.getMealPlanId())
                .orElseGet(() -> {
                    ShoppingList created = new ShoppingList();
                    created.setMealPlan(plan);
                    created.setOwner(plan.getUser());
                    created.setListName(plan.getPlanName() + " Shopping List");
                    return shoppingListRepository.save(created);
                });
    }

    private ShoppingListItem toPersistentItem(ShoppingList list, RecipeStepIngredient agg,
                                              User user, Set<Integer> checkedIds,
                                              Set<Integer> hiddenIds, int sortOrder) {
        ShoppingListItem item = new ShoppingListItem();
        item.setShoppingList(list);
        item.setIngredient(agg.getIngredient());
        item.setGroceryCategory(agg.getIngredient().getGroceryCategory());
        item.setAmount(agg.getAmount());
        item.setMeasurement(agg.getMeasurement());
        item.setNote(agg.getIngredientNote());
        item.setIsManualAddition(false);
        item.setAddedBy(user);
        item.setSortOrder(sortOrder);
        if (agg.getIngredient() != null) {
            Integer ingId = agg.getIngredient().getIngredientId();
            if (checkedIds.contains(ingId)) {
                item.setIsChecked(true);
            }
            if (hiddenIds.contains(ingId)) {
                item.setIsHidden(true);
            }
        }
        return item;
    }

    private GroceryCategory resolveCategory(Ingredient ingredient, GroceryCategory override) {
        if (override != null) return override;
        if (ingredient != null) return ingredient.getGroceryCategory();
        List<GroceryCategory> all = ingredientService.getAllGroceryCategoriesSortedByOrder();
        return all.stream()
                .filter(c -> c.getCategoryName().toLowerCase().contains("other"))
                .findFirst()
                .orElse(all.get(all.size() - 1));
    }

    @Transactional
    public void resetFromMealPlan(ShoppingList list, User triggeredBy) {
        if (list.getMealPlan() == null) {
            // Standalone list: uncheck/unhide everything and remove manual additions
            list.getItems().removeIf(item -> Boolean.TRUE.equals(item.getIsManualAddition()));
            list.getItems().forEach(item -> {
                item.setIsChecked(false);
                item.setIsHidden(false);
                item.setIsQuestioned(false);
            });
            shoppingListRepository.save(list);
            return;
        }

        // Wipe all items (clears checkmarks, hidden flags, and old extra items) and rebuild fresh
        list.getItems().clear();
        shoppingListRepository.saveAndFlush(list);

        List<MealPlanRecipe> scheduledEntries = list.getMealPlan().getPlannedRecipes().stream()
                .filter(e -> e.isScheduled())
                .toList();

        List<Recipe> recipes = recipeScaler.prepareRecipesForAggregation(scheduledEntries);
        Map<String, List<RecipeStepIngredient>> aggregated =
                RecipeUtility.aggregateAndPromoteShoppingList(recipes, metadataLookupService.getAllMeasurements());

        int order = 1;
        for (List<RecipeStepIngredient> aisleItems : aggregated.values()) {
            for (RecipeStepIngredient agg : aisleItems) {
                list.getItems().add(toPersistentItem(list, agg, triggeredBy, Collections.emptySet(), Collections.emptySet(), order++));
            }
        }
        shoppingListRepository.save(list);
    }

    @Transactional
    public void syncIfExistsForMealPlan(MealPlan plan, User triggeredBy) {
        shoppingListRepository.findFirstByMealPlan_MealPlanIdOrderByCreatedAtDesc(plan.getMealPlanId())
                .ifPresent(existingList -> {
                    List<MealPlanRecipe> scheduledEntries = plan.getPlannedRecipes().stream()
                            .filter(e -> e.isScheduled())
                            .toList();
                    generateOrSyncFromMealPlan(plan, scheduledEntries, triggeredBy);
                });
    }

    public List<ShoppingList> getAccessibleShoppingLists(Integer userId) {
        return shoppingListRepository.findAllAccessibleByUserId(userId);
    }
}
