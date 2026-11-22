package com.braindribbler.recipe.service.mealplan;

import com.braindribbler.recipe.domain.mealplan.MealPlanRecipe;
import com.braindribbler.recipe.domain.recipe.Recipe;
import com.braindribbler.recipe.domain.recipe.RecipeStep;
import com.braindribbler.recipe.domain.recipe.RecipeStepIngredient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class MealPlanRecipeScaler {

    public List<Recipe> prepareRecipesForAggregation(List<MealPlanRecipe> entries) {
        return entries.stream()
                .map(this::scaleIfOverridden)
                .toList();
    }

    public Recipe scaleIfOverridden(MealPlanRecipe entry) {
        Recipe original = entry.getRecipe();
        Integer override = entry.getServingsOverride();
        Integer baseServings = original.getServings();

        if (override == null || baseServings == null || baseServings <= 0 || override.equals(baseServings)) {
            return original;
        }

        BigDecimal ratio = BigDecimal.valueOf(override)
                .divide(BigDecimal.valueOf(baseServings), 6, RoundingMode.HALF_UP);

        Recipe copy = new Recipe();
        copy.setRecipeId(original.getRecipeId());
        copy.setTitle(original.getTitle());
        copy.setSteps(scaleSteps(original.getSteps(), ratio));
        return copy;
    }

    private List<RecipeStep> scaleSteps(List<RecipeStep> steps, BigDecimal ratio) {
        List<RecipeStep> scaledSteps = new ArrayList<>();
        for (RecipeStep step : steps) {
            RecipeStep stepCopy = new RecipeStep();
            stepCopy.setStepIngredients(scaleIngredients(step.getStepIngredients(), ratio));
            scaledSteps.add(stepCopy);
        }
        return scaledSteps;
    }

    private List<RecipeStepIngredient> scaleIngredients(List<RecipeStepIngredient> items, BigDecimal ratio) {
        List<RecipeStepIngredient> scaled = new ArrayList<>();
        if (items == null) {
            return scaled;
        }
        for (RecipeStepIngredient rsi : items) {
            RecipeStepIngredient copy = new RecipeStepIngredient();
            copy.setIngredient(rsi.getIngredient());
            copy.setMeasurement(rsi.getMeasurement());
            copy.setIngredientNote(rsi.getIngredientNote());

            BigDecimal rawAmt = BigDecimal.valueOf(((Number) rsi.getAmount()).doubleValue());
            copy.setAmount(rawAmt.multiply(ratio));
            scaled.add(copy);
        }
        return scaled;
    }
}
