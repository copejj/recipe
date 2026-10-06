package com.braindribbler.recipe.domain.recipe;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "recipe_step_ingredient", schema = "public")
public class RecipeStepIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipe_step_ingredient_id")
    private Integer recipeStepIngredientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_step_id", nullable = false)
    private RecipeStep recipeStep;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(name = "amount", nullable = false, precision = 6, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "measurement_id", nullable = false)
    private Measurement measurement;

    @Column(name = "ingredient_note", columnDefinition = "text")
    private String ingredientNote;

    public RecipeStepIngredient() {
    }

    public Integer getRecipeStepIngredientId() {
        return recipeStepIngredientId;
    }

    public void setRecipeStepIngredientId(Integer recipeStepIngredientId) {
        this.recipeStepIngredientId = recipeStepIngredientId;
    }

    public RecipeStep getRecipeStep() {
        return recipeStep;
    }

    public void setRecipeStep(RecipeStep recipeStep) {
        this.recipeStep = recipeStep;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public void setIngredient(Ingredient ingredient) {
        this.ingredient = ingredient;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Measurement getMeasurement() {
        return measurement;
    }

    public void setMeasurement(Measurement measurement) {
        this.measurement = measurement;
    }

    public String getIngredientNote() {
        return ingredientNote;
    }

    public void setIngredientNote(String ingredientNote) {
        this.ingredientNote = ingredientNote;
    }
}
