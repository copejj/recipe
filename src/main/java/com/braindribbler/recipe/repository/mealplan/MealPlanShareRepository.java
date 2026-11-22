package com.braindribbler.recipe.repository.mealplan;

import com.braindribbler.recipe.domain.mealplan.MealPlanShare;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MealPlanShareRepository extends JpaRepository<MealPlanShare, Integer> {
    Optional<MealPlanShare> findByMealPlan_MealPlanIdAndSharedUser_UserId(Integer mealPlanId, Integer userId);
}
