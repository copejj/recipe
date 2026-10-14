package com.braindribbler.recipe.dto.recipe;

import java.util.ArrayList;
import java.util.List;

public class StepForm {
    private String instructionText;
    private List<IngredientForm> ingredients = new ArrayList<>();

    public StepForm() {
        // Initialize with one blank ingredient entry per step
        this.ingredients.add(new IngredientForm());
    }

    // Getters and Setters...
    public String getInstructionText() {
        return instructionText;
    }

    public void setInstructionText(String instructionText) {
        this.instructionText = instructionText;
    }

    public List<IngredientForm> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<IngredientForm> ingredients) {
        this.ingredients = ingredients;
    }
}
