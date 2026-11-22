package com.braindribbler.recipe.domain.mealplan;

import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.domain.recipe.AccessLevel; // Adjust package if AccessLevel is in auth/security
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "meal_plan_shares")
public class MealPlanShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meal_plan_share_id")
    private Integer mealPlanShareId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_plan_id", nullable = false)
    private MealPlan mealPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_user_id")
    private User sharedUser;

    @Column(name = "shared_token_id")
    private UUID sharedTokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "access_level_id", nullable = false)
    private AccessLevel accessLevel;

    @Column(name = "shared_at", nullable = false, updatable = false)
    private OffsetDateTime sharedAt = OffsetDateTime.now();

    public MealPlanShare() {}

    // Getters and Setters
    public Integer getMealPlanShareId() { return mealPlanShareId; }
    public void setMealPlanShareId(Integer mealPlanShareId) { this.mealPlanShareId = mealPlanShareId; }

    public MealPlan getMealPlan() { return mealPlan; }
    public void setMealPlan(MealPlan mealPlan) { this.mealPlan = mealPlan; }

    public User getSharedUser() { return sharedUser; }
    public void setSharedUser(User sharedUser) { this.sharedUser = sharedUser; }

    public UUID getSharedTokenId() { return sharedTokenId; }
    public void setSharedTokenId(UUID sharedTokenId) { this.sharedTokenId = sharedTokenId; }

    public AccessLevel getAccessLevel() { return accessLevel; }
    public void setAccessLevel(AccessLevel accessLevel) { this.accessLevel = accessLevel; }

    public OffsetDateTime getSharedAt() { return sharedAt; }
    public void setSharedAt(OffsetDateTime sharedAt) { this.sharedAt = sharedAt; }
}
