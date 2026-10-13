package com.braindribbler.recipe.repository.recipe;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.braindribbler.recipe.domain.recipe.RecipeStepIngredient;

public interface RecipeStepIngredientRepository extends JpaRepository<RecipeStepIngredient, Integer> {
    List<RecipeStepIngredient> findByRecipeStepRecipeStepId(Long recipeStepId);
}
