package com.braindribbler.recipe.repository.recipe;

import org.springframework.data.jpa.repository.JpaRepository;

import com.braindribbler.recipe.domain.recipe.GroceryCategory;

public interface GroceryCategoryRepository extends JpaRepository<GroceryCategory, Integer> {
}
