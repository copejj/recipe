package com.braindribbler.recipe.domain.recipe;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recipe_step", schema = "public")
public class RecipeStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipe_step_id")
    private Long recipeStepId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(name = "step_number", nullable = false)
    private Integer stepNumber;

    @Column(name = "instruction_text", nullable = false, columnDefinition = "text")
    private String instructionText;

    @OneToMany(mappedBy = "recipeStep", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeStepIngredient> stepIngredients = new ArrayList<>();

    public RecipeStep() {
    }

    public Long getRecipeStepId() {
        return recipeStepId;
    }

    public void setRecipeStepId(Long recipeStepId) {
        this.recipeStepId = recipeStepId;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public Integer getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(Integer stepNumber) {
        this.stepNumber = stepNumber;
    }

    public String getInstructionText() {
        return instructionText;
    }

    public void setInstructionText(String instructionText) {
        this.instructionText = instructionText;
    }

    public List<RecipeStepIngredient> getStepIngredients() {
        return stepIngredients;
    }

    public void setStepIngredients(List<RecipeStepIngredient> stepIngredients) {
        this.stepIngredients = stepIngredients;
    }
}
