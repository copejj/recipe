package com.braindribbler.recipe.repository.recipe;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.domain.recipe.Ingredient;
import com.braindribbler.recipe.dto.recipe.PendingIngredientProjection;

public interface IngredientRepository extends JpaRepository<Ingredient, Integer> {
    Optional<Ingredient> findByIngredientNameIgnoreCase(String ingredientName);

    List<Ingredient> findByGroceryCategoryGroceryCategoryId(Integer groceryCategoryId);

    List<Ingredient> findAllByOrderByIngredientNameAsc();

    @Query(value = """
            SELECT
                i.ingredient_id as ingredientId,
                i.ingredient_name as ingredientName,
                c.category_name as categoryName,
                u.display_name as createdByUsername, -- ◄ FIXED: Uses display_name column
                i.created_at as createdAt
            FROM ingredients i
            LEFT JOIN grocery_category c ON i.grocery_category_id = c.grocery_category_id
            LEFT JOIN users u ON i.created_by_user_id = u.user_id
            WHERE i.is_verified = FALSE
            ORDER BY i.created_at ASC
            """, nativeQuery = true)
    List<PendingIngredientProjection> findUnverifiedIngredients();

    @Modifying
    @Query(value = """
            UPDATE recipe_step_ingredient
            SET ingredient_id = :targetId
            WHERE ingredient_id = :duplicateId
            """, nativeQuery = true)
    void relinkMergedIngredients(@Param("duplicateId") Integer duplicateId, @Param("targetId") Integer targetId);

    @Query("SELECT i FROM Ingredient i WHERE i.isVerified = true OR i.createdByUser = :user ORDER BY i.ingredientName ASC")
    List<Ingredient> findAvailableIngredientsForUser(@Param("user") User user);
}
