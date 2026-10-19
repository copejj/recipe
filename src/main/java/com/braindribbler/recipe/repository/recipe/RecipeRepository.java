package com.braindribbler.recipe.repository.recipe;

import com.braindribbler.recipe.domain.recipe.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecipeRepository extends JpaRepository<Recipe, Integer> {
        Optional<Recipe> findByPublicId(UUID publicId);

        List<Recipe> findByUserUserId(Long userId); // Fetches all recipes belonging to a specific user id

        List<Recipe> findByRecipeVisibilityVisibilityName(String visibilityName);

        // 1. Text Search utilizing UUID tracking and visibility checks
        @Query(value = "SELECT r.* FROM recipes r WHERE " +
                        "(r.title ILIKE CONCAT('%', :keyword, '%') OR r.description ILIKE CONCAT('%', :keyword, '%')) AND ("
                        +
                        "  r.recipe_visibility_id = 1 " +
                        "  OR (:isLoggedIn = true AND r.recipe_visibility_id = 2) " +
                        "  OR (:isLoggedIn = true AND r.recipe_visibility_id = 3 AND r.user_id = :currentUserId)" +
                        ")", nativeQuery = true)
        List<Recipe> searchTextRecipes(
                        @Param("keyword") String keyword,
                        @Param("isLoggedIn") boolean isLoggedIn,
                        @Param("currentUserId") Integer currentUserId);

        // 2. Ingredient matching search returning full recipes sorted by max matching
        // ingredients
        @Query(value = "SELECT r.* FROM recipes r " +
                        "JOIN recipe_step rs ON r.recipe_id = rs.recipe_id " +
                        "JOIN recipe_step_ingredient rsi ON rs.recipe_step_id = rsi.recipe_step_id " +
                        "WHERE rsi.ingredient_id IN (:ingredientIds) AND (" +
                        "  r.recipe_visibility_id = 1 " +
                        "  OR (:isLoggedIn = true AND r.recipe_visibility_id = 2) " +
                        "  OR (:isLoggedIn = true AND r.recipe_visibility_id = 3 AND r.user_id = :currentUserId)" +
                        ") " +
                        "GROUP BY r.recipe_id " +
                        "ORDER BY COUNT(DISTINCT rsi.ingredient_id) DESC", nativeQuery = true)
        List<Recipe> searchByIngredients(
                        @Param("ingredientIds") List<Integer> ingredientIds,
                        @Param("isLoggedIn") boolean isLoggedIn,
                        @Param("currentUserId") Integer currentUserId);

        // 3. Ingredient correlation matching ("What's in my fridge?") ranked by max
        // matching density
        @Query(value = "SELECT r.* FROM recipes r " +
                        "JOIN recipe_step rs ON r.recipe_id = rs.recipe_id " +
                        "JOIN recipe_step_ingredient rsi ON rs.recipe_step_id = rsi.recipe_step_id " +
                        "WHERE rsi.ingredient_id IN (:ingredientIds) AND (" +
                        "  r.recipe_visibility_id = 1 " +
                        "  OR (:isLoggedIn = true AND r.recipe_visibility_id = 2) " +
                        "  OR (:isLoggedIn = true AND r.recipe_visibility_id = 3 AND r.user_id = :currentUserId)" +
                        ") " +
                        "GROUP BY r.recipe_id " +
                        "ORDER BY COUNT(DISTINCT rsi.ingredient_id) DESC", nativeQuery = true)
        List<Recipe> searchRecipesByIngredientsList(
                        @Param("ingredientIds") List<Integer> ingredientIds,
                        @Param("isLoggedIn") boolean isLoggedIn,
                        @Param("currentUserId") Integer currentUserId);
}
