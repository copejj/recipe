package com.braindribbler.recipe.dto.recipe;

public record SavedRecipeSummary(
    Integer recipeId,
    String title,
    String authorName
) {}
