package com.braindribbler.recipe.domain.mealplan;

import com.braindribbler.recipe.domain.recipe.Recipe;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "meal_plan_recipes")
public class MealPlanRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meal_plan_recipe_id")
    private Integer mealPlanRecipeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_plan_id", nullable = false)
    private MealPlan mealPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(name = "day_of_week")
    private Short dayOfWeek; // 1 = Monday .. 7 = Sunday (used for recurring plans)

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate; // Specific date (used for calendar plans)

    @Column(name = "meal_type", columnDefinition = "text")
    private String mealType; // e.g. "Breakfast", "Lunch", "Dinner", "Snack"

    @Column(name = "servings_override")
    private Integer servingsOverride;

    @Column(name = "sort_order", nullable = false)
    private Short sortOrder = 1;

    public MealPlanRecipe() {}

    @Transient
    public boolean isScheduled() {
        return dayOfWeek != null || scheduledDate != null;
    }

    // Getters and Setters
    public Integer getMealPlanRecipeId() { return mealPlanRecipeId; }
    public void setMealPlanRecipeId(Integer mealPlanRecipeId) { this.mealPlanRecipeId = mealPlanRecipeId; }

    public MealPlan getMealPlan() { return mealPlan; }
    public void setMealPlan(MealPlan mealPlan) { this.mealPlan = mealPlan; }

    public Recipe getRecipe() { return recipe; }
    public void setRecipe(Recipe recipe) { this.recipe = recipe; }

    public Short getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Short dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public LocalDate getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDate scheduledDate) { this.scheduledDate = scheduledDate; }

    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }

    public Integer getServingsOverride() { return servingsOverride; }
    public void setServingsOverride(Integer servingsOverride) { this.servingsOverride = servingsOverride; }

    public Short getSortOrder() { return sortOrder; }
    public void setSortOrder(Short sortOrder) { this.sortOrder = sortOrder; }
}
