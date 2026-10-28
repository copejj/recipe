package com.braindribbler.recipe.domain.recipe;

import java.time.ZonedDateTime;

import com.braindribbler.recipe.domain.auth.User;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ingredients")
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ingredient_id")
    private Integer ingredientId;

    @Column(name = "ingredient_name", nullable = false, unique = true, columnDefinition = "text")
    private String ingredientName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grocery_category_id", nullable = false)
    private GroceryCategory groceryCategory;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = true)
    private User createdByUser;

    @Column(name = "created_by_user_id", insertable = false, updatable = false)
    private Integer createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private ZonedDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM) // <-- Add this line
    @Column(name = "classification", nullable = false)
    private IngredientClassification classification = IngredientClassification.SOLID;

    public Ingredient() {
    }

    public Integer getIngredientId() {
        return ingredientId;
    }

    public void setIngredientId(Integer ingredientId) {
        this.ingredientId = ingredientId;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
    }

    public GroceryCategory getGroceryCategory() {
        return groceryCategory;
    }

    public void setGroceryCategory(GroceryCategory groceryCategory) {
        this.groceryCategory = groceryCategory;
    }

    public Boolean getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(Boolean isVerified) {
        this.isVerified = isVerified;
    }

    public Boolean getIsLocked() {
        return isLocked;
    }

    public void setIsLocked(Boolean isLocked) {
        this.isLocked = isLocked;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }

    public Integer getCreatedByUserId() {
        return createdByUserId;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public IngredientClassification getClassification() {
        return classification;
    }

    public void setClassification(IngredientClassification classification) {
        this.classification = classification;
    }
}
