package com.braindribbler.recipe.repository.recipe;

import org.springframework.data.jpa.repository.JpaRepository;
import com.braindribbler.recipe.domain.recipe.RecipeCategory;
import java.util.List;
import java.util.Optional;

public interface RecipeCategoryRepository extends JpaRepository<RecipeCategory, Integer> {
    Optional<RecipeCategory> findByNameOrderByNameAsc(String name); 

    List<RecipeCategory> findAllByOrderByNameAsc();
}
