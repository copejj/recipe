package com.braindribbler.recipe.domain.recipe;

/**
 * Defines the core behavioral track of an ingredient.
 * Used by the frontend kitchen scaling engine to determine unit conversion
 * rules.
 */
public enum IngredientClassification {

    /**
     * Measured in physical units, slices, or straight absolute masses (e.g.,
     * Chicken Breast, Apples, Butter).
     */
    SOLID,

    /**
     * Volumetric liquid track. Allowed to climb up to bulk fluid quantities (e.g.,
     * Milk, Broth, Water).
     */
    WET,

    /**
     * Volumetric dry baking track. Gated at the Cup threshold to prevent Pint/Quart
     * breakouts (e.g., Flour, Sugar, Spices).
     */
    DRY
}
