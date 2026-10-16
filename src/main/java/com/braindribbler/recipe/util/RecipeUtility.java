package com.braindribbler.recipe.util;

import com.braindribbler.recipe.domain.recipe.Measurement;
import com.braindribbler.recipe.domain.recipe.Recipe;
import com.braindribbler.recipe.domain.recipe.RecipeStepIngredient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class RecipeUtility {

    public static String convertDecimalToKitchenFraction(Double amount) {
        if (amount == null || amount <= 0)
            return "";

        long wholePart = amount.longValue();
        double decimalPart = amount - wholePart;

        // Round to 3 decimal places to fix floating-point clipping anomalies
        decimalPart = Math.round(decimalPart * 1000.0) / 1000.0;

        if (decimalPart == 0)
            return String.valueOf(wholePart);

        String fractionText = "";
        if (decimalPart >= 0.08 && decimalPart <= 0.16)
            fractionText = "1/8";
        else if (decimalPart > 0.16 && decimalPart <= 0.29)
            fractionText = "1/4";
        else if (decimalPart > 0.29 && decimalPart <= 0.40)
            fractionText = "1/3";
        else if (decimalPart > 0.40 && decimalPart <= 0.58)
            fractionText = "1/2";
        else if (decimalPart > 0.58 && decimalPart <= 0.70)
            fractionText = "2/3";
        else if (decimalPart > 0.70 && decimalPart <= 0.82)
            fractionText = "3/4";
        else if (decimalPart > 0.82 && decimalPart <= 0.93)
            fractionText = "7/8";
        else if (decimalPart > 0.93)
            return String.valueOf(wholePart + 1);

        return (wholePart > 0 ? wholePart + " " : "") + fractionText;
    }

    /**
     * Converts a BigDecimal amount value to a clean kitchen fraction string.
     */
    public static String convertDecimalToKitchenFraction(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            return "";

        long wholePart = amount.longValue();
        // Subtract whole part to isolate decimal remainder
        BigDecimal decimalPartBd = amount.subtract(BigDecimal.valueOf(wholePart));

        // Round to 3 decimal places to smooth floating calculations
        double decimalPart = decimalPartBd.setScale(3, RoundingMode.HALF_UP).doubleValue();

        if (decimalPart == 0)
            return String.valueOf(wholePart);

        String fractionText = "";
        if (decimalPart >= 0.08 && decimalPart <= 0.16)
            fractionText = "1/8";
        else if (decimalPart > 0.16 && decimalPart <= 0.29)
            fractionText = "1/4";
        else if (decimalPart > 0.29 && decimalPart <= 0.40)
            fractionText = "1/3";
        else if (decimalPart > 0.40 && decimalPart <= 0.58)
            fractionText = "1/2";
        else if (decimalPart > 0.58 && decimalPart <= 0.70)
            fractionText = "2/3";
        else if (decimalPart > 0.70 && decimalPart <= 0.82)
            fractionText = "3/4";
        else if (decimalPart > 0.82 && decimalPart <= 0.93)
            fractionText = "7/8";
        else if (decimalPart > 0.93)
            return String.valueOf(wholePart + 1);

        return (wholePart > 0 ? wholePart + " " : "") + fractionText;
    }

    /**
     * Aggregates, normalizes, and auto-promotes multi-recipe ingredient batches
     * matching exact BigDecimal column attributes seamlessly.
     */
    public static Map<String, List<RecipeStepIngredient>> aggregateAndPromoteShoppingList(
            List<Recipe> recipes,
            List<Measurement> allMeasurementsPool) {

        if (recipes == null || recipes.isEmpty()) {
            return Collections.emptyMap();
        }

        // 1. Flatten all step lines
        List<RecipeStepIngredient> flatIngredients = recipes.stream()
                .filter(Objects::nonNull)
                .flatMap(r -> r.getSteps().stream())
                .filter(step -> step.getStepIngredients() != null)
                .flatMap(step -> step.getStepIngredients().stream())
                .filter(item -> item.getIngredient() != null && item.getMeasurement() != null)
                .toList();

        // 2. Accumulate base pools
        Map<Integer, BigDecimal> ingredientBaseSums = new HashMap<>();
        Map<Integer, Measurement> referenceMeasurements = new HashMap<>();
        Map<Integer, List<String>> accumulatedNotes = new HashMap<>();

        for (RecipeStepIngredient item : flatIngredients) {
            Integer ingId = item.getIngredient().getIngredientId();
            Measurement unit = item.getMeasurement();

            // --- FIXED: Read direct BigDecimal or map primitives cleanly without loops ---
            BigDecimal amountBd = (item.getAmount() instanceof BigDecimal) ? (BigDecimal) item.getAmount()
                    : BigDecimal.valueOf(((Number) item.getAmount()).doubleValue());

            BigDecimal baseAmount = amountBd.multiply(unit.getBaseUnitEquivalent());

            ingredientBaseSums.put(ingId, ingredientBaseSums.getOrDefault(ingId, BigDecimal.ZERO).add(baseAmount));
            referenceMeasurements.put(ingId, unit);

            if (item.getIngredientNote() != null && !item.getIngredientNote().trim().isEmpty()) {
                accumulatedNotes.computeIfAbsent(ingId, k -> new ArrayList<>()).add(item.getIngredientNote().trim());
            }
        }

        // 3. Auto-Promote
        List<RecipeStepIngredient> finalShoppingList = new ArrayList<>();

        for (Map.Entry<Integer, BigDecimal> entry : ingredientBaseSums.entrySet()) {
            Integer ingId = entry.getKey();
            BigDecimal totalBaseAmount = entry.getValue();
            Measurement originalUnit = referenceMeasurements.get(ingId);
            Integer targetBaseTypeId = originalUnit.getBaseType().getBaseTypeId();

            List<Measurement> candidateUnits = allMeasurementsPool.stream()
                    .filter(m -> m.getBaseType().getBaseTypeId().equals(targetBaseTypeId))
                    .sorted((m1, m2) -> Short.compare(m2.getScalingRank(), m1.getScalingRank()))
                    .toList();

            Measurement optimizedTargetUnit = originalUnit;
            BigDecimal scaledFinalAmount = totalBaseAmount;

            for (Measurement candidate : candidateUnits) {
                BigDecimal threshold = candidate.getBaseUnitEquivalent().multiply(BigDecimal.valueOf(0.85));
                if (totalBaseAmount.compareTo(threshold) >= 0) {
                    optimizedTargetUnit = candidate;
                    scaledFinalAmount = totalBaseAmount.divide(candidate.getBaseUnitEquivalent(), 4,
                            RoundingMode.HALF_UP);
                    break;
                }
            }

            // Build output entities safely
            RecipeStepIngredient shoppingItem = new RecipeStepIngredient();
            RecipeStepIngredient sample = flatIngredients.stream()
                    .filter(i -> i.getIngredient().getIngredientId().equals(ingId))
                    .findFirst()
                    .orElse(flatIngredients.get(0));

            shoppingItem.setIngredient(sample.getIngredient());
            shoppingItem.setMeasurement(optimizedTargetUnit);

            // --- FIXED: Pass the clean BigDecimal object straight to your entity field ---
            shoppingItem.setAmount(scaledFinalAmount);

            if (accumulatedNotes.containsKey(ingId)) {
                shoppingItem.setIngredientNote(String.join(", ", accumulatedNotes.get(ingId)));
            }

            finalShoppingList.add(shoppingItem);
        }

        // 4. FIXED: Group, sort by display_order priority first, and sort by ingredient
        // name alphabetically second
        return finalShoppingList.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getIngredient().getGroceryCategory(), // Group by the full Entity object
                        Collectors.toList()))
                .entrySet().stream()
                .sorted((entry1, entry2) -> {
                    // Sort 1: Compare Category display_order priorities numerical ranks
                    int order1 = entry1.getKey().getDisplayOrder() != null ? entry1.getKey().getDisplayOrder() : 999;
                    int order2 = entry2.getKey().getDisplayOrder() != null ? entry2.getKey().getDisplayOrder() : 999;
                    int comp = Integer.compare(order1, order2);

                    // Sort 2: If display orders match exactly, fall back to Alphabetical Category
                    // names
                    if (comp == 0) {
                        comp = entry1.getKey().getCategoryName().compareToIgnoreCase(entry2.getKey().getCategoryName());
                    }
                    return comp;
                })
                .collect(Collectors.toMap(
                        entry -> entry.getKey().getCategoryName(), // Extract name string for Thymeleaf UI rendering
                        entry -> {
                            List<RecipeStepIngredient> list = entry.getValue();
                            // Sort 3: Sort inner ingredients alphabetically by name (A-Z)
                            list.sort(Comparator
                                    .comparing(item -> item.getIngredient().getIngredientName().toLowerCase()));
                            return list;
                        },
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new // LinkedHashMap strictly preserves our custom double-sorted query order!
                ));

    }
}
