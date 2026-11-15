// IngredientClassification.java
package com.braindribbler.recipe.domain.recipe;

public enum IngredientClassification {

    WET("Measured in pints, quarts, gallons, or fluid ounces (plus weight & count)"),
    DRY("Dry volumes (tsp, tbsp, cup), weights (oz, lb), or counts (whole, slice)");

    private final String description;

    IngredientClassification(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }
}
