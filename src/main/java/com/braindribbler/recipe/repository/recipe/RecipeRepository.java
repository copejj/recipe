package com.braindribbler.recipe.repository.recipe;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.braindribbler.recipe.domain.recipe.Recipe;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    Optional<Recipe> findByPublicId(UUID publicId);

    List<Recipe> findByUserUserId(Long userId); // Fetches all recipes belonging to a specific user id

    List<Recipe> findByRecipeVisibilityVisibilityName(String visibilityName);
}
