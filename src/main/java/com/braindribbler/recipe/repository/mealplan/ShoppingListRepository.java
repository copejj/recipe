package com.braindribbler.recipe.repository.mealplan;

import com.braindribbler.recipe.domain.mealplan.ShoppingList;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShoppingListRepository extends JpaRepository<ShoppingList, Integer> {
    Optional<ShoppingList> findByPublicId(UUID publicId);
    Optional<ShoppingList> findFirstByMealPlan_MealPlanIdOrderByCreatedAtDesc(Integer mealPlanId);
    List<ShoppingList> findByOwner_UserIdOrderByCreatedAtDesc(Integer ownerUserId);
}
