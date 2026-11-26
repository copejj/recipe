package com.braindribbler.recipe.repository.mealplan;

import com.braindribbler.recipe.domain.mealplan.ShoppingListShare;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ShoppingListShareRepository extends JpaRepository<ShoppingListShare, Integer> {
    Optional<ShoppingListShare> findByShoppingList_ShoppingListIdAndSharedUser_UserId(Integer shoppingListId, Integer userId);
}
