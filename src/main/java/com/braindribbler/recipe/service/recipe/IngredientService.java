package com.braindribbler.recipe.service.recipe;

import com.braindribbler.recipe.dto.recipe.PendingIngredientProjection;
import java.util.List;

public interface IngredientService {
    List<PendingIngredientProjection> getUnverifiedIngredients();

    void approveIngredient(Integer ingredientId);

    void mergeAndRejectIngredient(Integer duplicateIngredientId, Integer targetIngredientId);
}
