package com.braindribbler.recipe.domain.mealplan;

import com.braindribbler.recipe.domain.auth.User;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "meal_plans")
public class MealPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meal_plan_id")
    private Integer mealPlanId;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "plan_name", nullable = false, columnDefinition = "text")
    private String planName;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "is_recurring", nullable = false)
    private Boolean isRecurring = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "mealPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<MealPlanRecipe> plannedRecipes = new ArrayList<>();

    @OneToMany(mappedBy = "mealPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MealPlanShare> shares = new ArrayList<>();

    public MealPlan() {}

    // Getters and Setters
    public Integer getMealPlanId() { return mealPlanId; }
    public void setMealPlanId(Integer mealPlanId) { this.mealPlanId = mealPlanId; }

    public UUID getPublicId() { return publicId; }
    public void setPublicId(UUID publicId) { this.publicId = publicId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public Boolean getIsRecurring() { return isRecurring; }
    public void setIsRecurring(Boolean isRecurring) { this.isRecurring = isRecurring; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public List<MealPlanRecipe> getPlannedRecipes() { return plannedRecipes; }
    public void setPlannedRecipes(List<MealPlanRecipe> plannedRecipes) { this.plannedRecipes = plannedRecipes; }

    public List<MealPlanShare> getShares() { return shares; }
    public void setShares(List<MealPlanShare> shares) { this.shares = shares; }
}
