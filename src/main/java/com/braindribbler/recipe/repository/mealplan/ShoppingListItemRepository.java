package com.braindribbler.recipe.repository.mealplan;

import com.braindribbler.recipe.domain.mealplan.ShoppingListItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItem, Integer> {
}
