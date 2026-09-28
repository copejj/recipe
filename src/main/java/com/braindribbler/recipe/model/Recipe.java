package com.braindribbler.recipe.model;

public class Recipe {
    private String title;
    private String instructions;

    public Recipe(String title, String instructions) {
        this.title = title;
        this.instructions = instructions;
    }

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
}

