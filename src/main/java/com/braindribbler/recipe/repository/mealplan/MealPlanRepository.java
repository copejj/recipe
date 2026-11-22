package com.braindribbler.recipe.repository.mealplan;

import com.braindribbler.recipe.domain.mealplan.MealPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MealPlanRepository extends JpaRepository<MealPlan, Integer> {

    Optional<MealPlan> findByPublicId(UUID publicId);

    // Plans owned by user OR shared directly with user
    @Query("""
        SELECT DISTINCT mp FROM MealPlan mp
        LEFT JOIN mp.shares s
        WHERE mp.user.userId = :userId OR s.sharedUser.userId = :userId
        ORDER BY mp.createdAt DESC
    """)
    List<MealPlan> findAllAccessibleByUserId(@Param("userId") Integer userId);
}
