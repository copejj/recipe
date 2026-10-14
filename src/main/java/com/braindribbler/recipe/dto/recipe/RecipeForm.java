package com.braindribbler.recipe.dto.recipe;

import java.util.ArrayList;
import java.util.List;

public class RecipeForm {
    private String title;
    private String description;
    private Integer servings;
    private String recipeNote;
    private Integer recipeVisibilityId;
    private List<StepForm> steps = new ArrayList<>();

    public RecipeForm() {
        // Initialize with one blank step so the form loads with fields ready
        this.steps.add(new StepForm());
    }

    // Getters and Setters...
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getServings() {
        return servings;
    }

    public void setServings(Integer servings) {
        this.servings = servings;
    }

    public String getRecipeNote() {
        return recipeNote;
    }

    public void setRecipeNote(String recipeNote) {
        this.recipeNote = recipeNote;
    }

    public Integer getRecipeVisibilityId() {
        return recipeVisibilityId;
    }

    public void setRecipeVisibilityId(Integer recipeVisibilityId) {
        this.recipeVisibilityId = recipeVisibilityId;
    }

    public List<StepForm> getSteps() {
        return steps;
    }

    public void setSteps(List<StepForm> steps) {
        this.steps = steps;
    }
}
