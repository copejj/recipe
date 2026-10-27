package com.braindribbler.recipe.repository.recipe;

import com.braindribbler.recipe.domain.recipe.Recipe;
import com.braindribbler.recipe.dto.recipe.RecipeStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecipeRepository extends JpaRepository<Recipe, Integer> {
        Optional<Recipe> findByPublicId(UUID publicId);

        List<Recipe> findByUserUserId(Long userId);

        List<Recipe> findByRecipeVisibilityVisibilityName(String visibilityName);

        // 1. Fetches all comprehensive public/owned recipes for the main feed
        @Query(value = "SELECT r.* FROM recipes r WHERE " +
                        "(" +
                        "  r.recipe_visibility_id = 1 " + // Public: Anyone can see
                        "  OR (:isLoggedIn = true AND r.recipe_visibility_id = 2 AND r.user_id = :currentUserId) " +
                        ")", nativeQuery = true)
        List<Recipe> findAllPublicOrOwned(
                        @Param("isLoggedIn") boolean isLoggedIn,
                        @Param("currentUserId") Integer currentUserId);

        // 2. Ingredient correlation matching ("What's in my fridge?") ranked by max
        // matching density
        @Query(value = "SELECT r.* FROM recipes r " +
                        "JOIN recipe_step rs ON r.recipe_id = rs.recipe_id " +
                        "JOIN recipe_step_ingredient rsi ON rs.recipe_step_id = rsi.recipe_step_id " +
                        "WHERE rsi.ingredient_id IN (:ingredientIds) AND (" +
                        "  r.recipe_visibility_id = 1 " + // Public: Anyone can see
                        "  OR (:isLoggedIn = true AND r.recipe_visibility_id = 2 AND r.user_id = :currentUserId) " +
                        ") " +
                        "GROUP BY r.recipe_id " +
                        "ORDER BY COUNT(DISTINCT rsi.ingredient_id) DESC", nativeQuery = true)
        List<Recipe> searchRecipesByIngredientsList(
                        @Param("ingredientIds") List<Integer> ingredientIds,
                        @Param("isLoggedIn") boolean isLoggedIn,
                        @Param("currentUserId") Integer currentUserId);

        // 3. Clean JPQL Aggregate Constructor query with COALESCE fallback wrappers
        @Query("SELECT new com.braindribbler.recipe.dto.recipe.RecipeStats(" +
                        "  CAST(COUNT(r) AS integer), " +
                        "  CAST(COALESCE(SUM(CASE WHEN r.recipeVisibility.recipeVisibilityId = 1 THEN 1 ELSE 0 END), 0) AS integer), "
                        +
                        "  CAST(COALESCE(SUM(CASE WHEN r.recipeVisibility.recipeVisibilityId = 3 THEN 1 ELSE 0 END), 0) AS integer), "
                        +
                        "  CAST(COALESCE(SUM(CASE WHEN r.recipeVisibility.recipeVisibilityId = 2 THEN 1 ELSE 0 END), 0) AS integer)"
                        +
                        ") " +
                        "FROM Recipe r WHERE r.user.userId = :userId")
        RecipeStats getRecipeStatsByUserId(@Param("userId") Integer userId);
}
