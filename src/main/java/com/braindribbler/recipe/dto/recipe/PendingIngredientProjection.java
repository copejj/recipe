package com.braindribbler.recipe.dto.recipe;

import java.time.ZonedDateTime;

public interface PendingIngredientProjection {
    Integer getIngredientId();

    String getIngredientName();

    String getCategoryName();

    String getCreatedByUsername();

    ZonedDateTime getCreatedAt();
}
