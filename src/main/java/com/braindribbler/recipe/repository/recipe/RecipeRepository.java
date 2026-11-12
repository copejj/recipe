package com.braindribbler.recipe.repository.recipe;

import com.braindribbler.recipe.domain.recipe.Recipe;
import com.braindribbler.recipe.dto.recipe.RecipeSaveBreakdown;
import com.braindribbler.recipe.dto.recipe.RecipeStats;
import com.braindribbler.recipe.dto.recipe.SavedRecipeSummary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecipeRepository extends JpaRepository<Recipe, Integer> {
        Optional<Recipe> findByPublicId(UUID publicId);

        List<Recipe> findByUserUserId(Long userId);

        List<Recipe> findByRecipeVisibilityVisibilityName(String visibilityName);

        List<Recipe> findByRecipeCategory_RecipeCategoryId(Integer recipeCategoryId);

        // 1. Fetches all comprehensive public/owned recipes for the main feed
        @Query(value = """
                SELECT r.* FROM recipes r
                JOIN recipe_visibility rv ON r.recipe_visibility_id = rv.recipe_visibility_id
                WHERE (
                rv.visibility_name = 'public'
                OR (:isLoggedIn = true AND rv.visibility_name = 'private' AND r.user_id = :currentUserId)
                )
                """, nativeQuery = true)
        List<Recipe> findAllPublicOrOwned(
                @Param("isLoggedIn") boolean isLoggedIn,
                @Param("currentUserId") Integer currentUserId);

        // 2. Ingredient correlation matching ("What's in my fridge?") ranked by max matching density
        @Query(value = """
                SELECT r.* FROM recipes r
                JOIN recipe_visibility rv ON r.recipe_visibility_id = rv.recipe_visibility_id
                JOIN recipe_step rs ON r.recipe_id = rs.recipe_id
                JOIN recipe_step_ingredient rsi ON rs.recipe_step_id = rsi.recipe_step_id
                WHERE rsi.ingredient_id IN (:ingredientIds) AND (
                rv.visibility_name = 'public'
                OR (:isLoggedIn = true AND rv.visibility_name = 'private' AND r.user_id = :currentUserId)
                )
                GROUP BY r.recipe_id
                ORDER BY COUNT(DISTINCT rsi.ingredient_id) DESC
                """, nativeQuery = true)
        List<Recipe> searchRecipesByIngredientsList(
                @Param("ingredientIds") List<Integer> ingredientIds,
                @Param("isLoggedIn") boolean isLoggedIn,
                @Param("currentUserId") Integer currentUserId);

        // 3. Clean JPQL Aggregate Constructor query with "Saved Me" and "I Saved" counts
        @Query("""
                SELECT new com.braindribbler.recipe.dto.recipe.RecipeStats(
                CAST(COUNT(r) AS integer),
                CAST(COALESCE(SUM(CASE WHEN r.recipeVisibility.visibilityName = 'public' THEN 1 ELSE 0 END), 0) AS integer),
                CAST(COALESCE(SUM(CASE WHEN r.recipeVisibility.visibilityName = 'shared' THEN 1 ELSE 0 END), 0) AS integer),
                CAST(COALESCE(SUM(CASE WHEN r.recipeVisibility.visibilityName = 'private' THEN 1 ELSE 0 END), 0) AS integer),
                CAST(COALESCE((SELECT COUNT(su) FROM Recipe r2 JOIN r2.savedByUsers su WHERE r2.user.userId = :userId AND su.userId <> :userId), 0) AS integer),
                CAST(COALESCE((SELECT COUNT(r3) FROM Recipe r3 JOIN r3.savedByUsers sbu WHERE sbu.userId = :userId AND (r3.user.userId = :userId OR r3.recipeVisibility.visibilityName <> 'private')), 0) AS integer)
                )
                FROM Recipe r WHERE r.user.userId = :userId
                """)
        RecipeStats getRecipeStatsByUserId(@Param("userId") Integer userId);


        // 4. Per-recipe save count breakdown (includes recipes with 0 saves)
        @Query("""
        SELECT new com.braindribbler.recipe.dto.recipe.RecipeSaveBreakdown(
                r.recipeId,
                r.publicId,
                r.title,
                v.visibilityName,
                r.viewCount,
                r.sharedViewCount,
                r.shareCount,
                COUNT(u.userId)
        )
        FROM Recipe r
        JOIN r.recipeVisibility v
        LEFT JOIN r.savedByUsers u
        WHERE r.user.userId = :userId
        GROUP BY r.recipeId, r.publicId, r.title, v.visibilityName, r.viewCount, r.sharedViewCount, r.shareCount
        ORDER BY COUNT(u.userId) DESC, r.viewCount DESC
        """)
        List<RecipeSaveBreakdown> getRecipeSaveBreakdownByUserId(@Param("userId") Integer userId);


        // 5. Recipes saved by the current user (excludes recipes made private by another author)
        @Query("""
                SELECT new com.braindribbler.recipe.dto.recipe.SavedRecipeSummary(
                r.recipeId,
                r.title,
                author.displayName
                )
                FROM Recipe r
                JOIN r.user author
                JOIN r.savedByUsers sbu
                WHERE sbu.userId = :userId
                AND (author.userId = :userId OR r.recipeVisibility.visibilityName <> 'private')
                ORDER BY r.title ASC
                """)
        List<SavedRecipeSummary> findSavedRecipesByUserId(@Param("userId") Integer userId);

        // 6. Fetches recipes owned by or saved by the user for their cookbook
        @Query("""
                SELECT r FROM Recipe r
                LEFT JOIN r.user u
                WHERE u.userId = :userId
                OR (r.recipeVisibility.visibilityName = 'public' AND :userId IN (SELECT su.userId FROM r.savedByUsers su))
                """)
        List<Recipe> findCookbookRecipes(@Param("userId") Integer userId);

        @Modifying
        @Transactional
        @Query(value = """
                INSERT INTO recipe_saves (user_id, recipe_id)
                VALUES (:userId, :recipeId)
                ON CONFLICT DO NOTHING
                """, nativeQuery = true)
        void nativeSaveRecipe(@Param("userId") Integer userId, @Param("recipeId") Integer recipeId);

        @Modifying
        @Transactional
        @Query(value = """
                DELETE FROM recipe_saves
                WHERE user_id = :userId AND recipe_id = :recipeId
                """, nativeQuery = true)
        void nativeUnsaveRecipe(@Param("userId") Integer userId, @Param("recipeId") Integer recipeId);

        @Modifying
        @Transactional
        @Query("UPDATE Recipe r SET r.viewCount = r.viewCount + 1 WHERE r.recipeId = :recipeId")
        void incrementViewCount(@Param("recipeId") Integer recipeId);

        @Modifying
        @Transactional
        @Query("UPDATE Recipe r SET r.sharedViewCount = r.sharedViewCount + 1 WHERE r.recipeId = :recipeId")
        void incrementSharedViewCount(@Param("recipeId") Integer recipeId);

        @Modifying
        @Transactional
        @Query("UPDATE Recipe r SET r.shareCount = r.shareCount + 1 WHERE r.recipeId = :recipeId")
        void incrementShareCount(@Param("recipeId") Integer recipeId);

}
