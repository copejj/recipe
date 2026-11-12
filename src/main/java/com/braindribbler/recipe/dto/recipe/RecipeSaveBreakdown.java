package com.braindribbler.recipe.dto.recipe;

import java.util.UUID;

public class RecipeSaveBreakdown {
    private final Integer recipeId;
    private final UUID publicId;
    private final String title;
    private final String visibilityName;
    private final Integer viewCount;
    private final Integer shareCount;
    private final Integer sharedViewCount;
    private final Long saveCount;

    public RecipeSaveBreakdown(
            Integer recipeId,
            UUID publicId,
            String title,
            String visibilityName,
            Integer viewCount,
            Integer sharedViewCount,
            Integer shareCount,
            Long saveCount) {
        this.recipeId = recipeId;
        this.publicId = publicId;
        this.title = title;
        this.visibilityName = visibilityName;
        this.viewCount = viewCount != null ? viewCount : 0;
        this.sharedViewCount = sharedViewCount != null ? sharedViewCount : 0;
        this.shareCount = shareCount != null ? shareCount : 0;
        this.saveCount = saveCount != null ? saveCount : 0L;
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

    public Integer getViewCount() {
        return viewCount;
    }

    public Integer getShareCount() {
        return shareCount;
    }

    public Integer getSharedViewCount() {
        return sharedViewCount;
    }

    public Long getSaveCount() {
        return saveCount;
    }
}
