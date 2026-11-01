package com.braindribbler.recipe.repository.recipe;

import org.springframework.data.jpa.repository.JpaRepository;

import com.braindribbler.recipe.domain.recipe.RecipeCategory;

import java.util.Optional;

public interface RecipeCategoryRepository extends JpaRepository<RecipeCategory, Integer> {
    
    // Derived query to find a category by its unique name string
    Optional<RecipeCategory> findByName(String name);
}
