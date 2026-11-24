package com.braindribbler.recipe.repository.mealplan;

import com.braindribbler.recipe.domain.mealplan.ShoppingList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShoppingListRepository extends JpaRepository<ShoppingList, Integer> {
    Optional<ShoppingList> findByPublicId(UUID publicId);
    Optional<ShoppingList> findFirstByMealPlan_MealPlanIdOrderByCreatedAtDesc(Integer mealPlanId);
    List<ShoppingList> findByOwner_UserIdOrderByCreatedAtDesc(Integer ownerUserId);

    @Query("""
        SELECT DISTINCT sl FROM ShoppingList sl
        LEFT JOIN sl.mealPlan mp
        LEFT JOIN mp.shares s
        WHERE sl.owner.userId = :userId
           OR mp.user.userId = :userId
           OR s.sharedUser.userId = :userId
        ORDER BY sl.createdAt DESC
    """)
    List<ShoppingList> findAllAccessibleByUserId(@Param("userId") Integer userId);
}
