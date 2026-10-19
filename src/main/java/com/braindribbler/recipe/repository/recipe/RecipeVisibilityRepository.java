package com.braindribbler.recipe.repository.recipe;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.braindribbler.recipe.domain.recipe.RecipeVisibility;

public interface RecipeVisibilityRepository extends JpaRepository<RecipeVisibility, Integer> {
    Optional<RecipeVisibility> findByVisibilityName(String visibilityName);
}
