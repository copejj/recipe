package com.braindribbler.recipe.dto.recipe;

import java.util.UUID;

public class RecipeSaveBreakdown {
    private final Integer recipeId;
    private final UUID publicId;
    private final String title;
    private final String visibilityName;
    private final Long saveCount;

    public RecipeSaveBreakdown(Integer recipeId, UUID publicId, String title, String visibilityName, Long saveCount) {
        this.recipeId = recipeId;
        this.publicId = publicId;
        this.title = title;
        this.visibilityName = visibilityName;
        this.saveCount = saveCount;
    }

    public Integer getRecipeId() {
        return recipeId;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public String getTitle() {
        return title;
    }

    public String getVisibilityName() {
        return visibilityName;
    }

    public Long getSaveCount() {
        return saveCount;
    }
}
