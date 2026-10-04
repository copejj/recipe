package com.braindribbler.recipe.domain.recipe;

import jakarta.persistence.*;

@Entity
@Table(name = "ingredients", schema = "public")
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ingredient_id")
    private Long ingredientId;

    @Column(name = "ingredient_name", nullable = false, unique = true)
    private String ingredientName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grocery_category_id", nullable = false)
    private GroceryCategory groceryCategory;

    public Ingredient() {
    }

    public Long getIngredientId() {
        return ingredientId;
    }

    public void setIngredientId(Long ingredientId) {
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
}
