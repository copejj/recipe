package com.braindribbler.recipe.repository.mealplan;

import com.braindribbler.recipe.domain.mealplan.MealPlanRecipe;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealPlanRecipeRepository extends JpaRepository<MealPlanRecipe, Integer> {
}
