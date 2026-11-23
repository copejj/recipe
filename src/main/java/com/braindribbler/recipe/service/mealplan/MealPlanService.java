package com.braindribbler.recipe.service.mealplan;

import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.domain.mealplan.*;
import com.braindribbler.recipe.domain.recipe.AccessLevel;
import com.braindribbler.recipe.domain.recipe.Recipe;
import com.braindribbler.recipe.repository.mealplan.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class MealPlanService {

    private final MealPlanRepository mealPlanRepository;
    private final MealPlanRecipeRepository mealPlanRecipeRepository;
    private final MealPlanShareRepository mealPlanShareRepository;

    public MealPlanService(MealPlanRepository mealPlanRepository,
                           MealPlanRecipeRepository mealPlanRecipeRepository,
                           MealPlanShareRepository mealPlanShareRepository) {
        this.mealPlanRepository = mealPlanRepository;
        this.mealPlanRecipeRepository = mealPlanRecipeRepository;
        this.mealPlanShareRepository = mealPlanShareRepository;
    }

    public List<MealPlan> getAccessibleMealPlans(Integer userId) {
        return mealPlanRepository.findAllAccessibleByUserId(userId);
    }

    public MealPlan getByPublicId(UUID publicId) {
        return mealPlanRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Meal plan not found"));
    }

    @Transactional
    public MealPlan createMealPlan(User owner, String planName, String description, LocalDate startDate, boolean isRecurring) {
        MealPlan plan = new MealPlan();
        plan.setUser(owner);
        plan.setPlanName(planName.trim());
        plan.setDescription(description != null ? description.trim() : null);
        plan.setStartDate(startDate);
        plan.setIsRecurring(isRecurring);
        return mealPlanRepository.save(plan);
    }

    /**
     * Adds a recipe to the Meal Plan's unscheduled pool (or directly to a day if provided).
     */
    @Transactional
    public MealPlanRecipe addRecipeToPlan(MealPlan plan, Recipe recipe, Short dayOfWeek, LocalDate scheduledDate, String mealType, Integer servingsOverride) {
        MealPlanRecipe entry = new MealPlanRecipe();
        entry.setMealPlan(plan);
        entry.setRecipe(recipe);
        entry.setDayOfWeek(dayOfWeek);
        entry.setScheduledDate(scheduledDate);
        entry.setMealType(mealType);
        entry.setServingsOverride(servingsOverride);
        entry.setSortOrder((short) (plan.getPlannedRecipes().size() + 1));
        plan.getPlannedRecipes().add(entry);
        return mealPlanRecipeRepository.save(entry);
    }

    /**
     * Assigns or moves a planned recipe entry onto a specific day/date & meal slot.
     */
    @Transactional
    public void scheduleEntry(Integer mealPlanRecipeId, Short dayOfWeek, LocalDate scheduledDate, String mealType, Integer servingsOverride) {
        MealPlanRecipe entry = mealPlanRecipeRepository.findById(mealPlanRecipeId)
                .orElseThrow(() -> new EntityNotFoundException("Planned recipe entry not found"));
        entry.setDayOfWeek(dayOfWeek);
        entry.setScheduledDate(scheduledDate);
        entry.setMealType(mealType);
        entry.setServingsOverride(servingsOverride);
        mealPlanRecipeRepository.save(entry);
    }

    @Transactional
    public void removeEntry(MealPlan plan, Integer mealPlanRecipeId) {
        plan.getPlannedRecipes().removeIf(entry -> entry.getMealPlanRecipeId().equals(mealPlanRecipeId));
        mealPlanRepository.save(plan);
    }

    @Transactional
    public void shareWithUser(MealPlan plan, User targetUser, AccessLevel accessLevel) {
        MealPlanShare share = mealPlanShareRepository
                .findByMealPlan_MealPlanIdAndSharedUser_UserId(plan.getMealPlanId(), targetUser.getUserId())
                .orElseGet(MealPlanShare::new);

        share.setMealPlan(plan);
        share.setSharedUser(targetUser);
        share.setAccessLevel(accessLevel);
        mealPlanShareRepository.save(share);
    }

    public boolean canUserView(MealPlan plan, User user) {
        if (user == null) return false;
        if (plan.getUser().getUserId().equals(user.getUserId())) return true;
        return plan.getShares().stream()
                .anyMatch(s -> s.getSharedUser() != null && s.getSharedUser().getUserId().equals(user.getUserId()));
    }

    public boolean canUserEdit(MealPlan plan, User user) {
        if (user == null) return false;
        if (plan.getUser().getUserId().equals(user.getUserId())) return true;
        return plan.getShares().stream()
                .filter(s -> s.getSharedUser() != null && s.getSharedUser().getUserId().equals(user.getUserId()))
                .anyMatch(s -> {
                    String level = s.getAccessLevel() != null ? s.getAccessLevel().getAccessName() : "";
                    return "EDIT".equalsIgnoreCase(level) || "EDITOR".equalsIgnoreCase(level);
                });
    }


    @Transactional
    public void removeShare(MealPlan plan, Integer shareId) {
        plan.getShares().removeIf(s -> s.getMealPlanShareId().equals(shareId));
        mealPlanRepository.save(plan);
    }
}
