package com.braindribbler.recipe.repository.recipe;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.braindribbler.recipe.domain.recipe.Ingredient;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    Optional<Ingredient> findByIngredientNameIgnoreCase(String ingredientName);

    List<Ingredient> findByGroceryCategoryGroceryCategoryId(Integer groceryCategoryId);
}
