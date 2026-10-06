package com.braindribbler.recipe.repository.recipe;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.braindribbler.recipe.domain.recipe.RecipeStep;

public interface RecipeStepRepository extends JpaRepository<RecipeStep, Long> {
    List<RecipeStep> findByRecipeRecipeIdOrderByStepNumberAsc(Long recipeId); // Resolves ordered lists natively
}
