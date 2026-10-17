package com.braindribbler.recipe.domain.recipe;

public enum IngredientClassification {

    SOLID("Absolute masses, items, counts"),
    WET("Volumetric fluid units"),
    DRY("Volumetric dry baking units");

    private final String description;

    IngredientClassification(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }
}
