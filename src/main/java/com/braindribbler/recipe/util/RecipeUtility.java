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
     * driven entirely by your database is_promotable and is_metric configurations.
     */
    public static Map<String, List<RecipeStepIngredient>> aggregateAndPromoteShoppingList(
            List<Recipe> recipes,
            List<Measurement> allMeasurementsPool) {

        if (recipes == null || recipes.isEmpty()) {
            return Collections.emptyMap();
        }

        // 1. Flatten all step ingredient lines
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

            BigDecimal amountBd = BigDecimal.valueOf(((Number) item.getAmount()).doubleValue());
            BigDecimal baseAmount = amountBd.multiply(unit.getBaseUnitEquivalent());

            ingredientBaseSums.put(ingId, ingredientBaseSums.getOrDefault(ingId, BigDecimal.ZERO).add(baseAmount));
            referenceMeasurements.put(ingId, unit);

            if (item.getIngredientNote() != null && !item.getIngredientNote().trim().isEmpty()) {
                accumulatedNotes.computeIfAbsent(ingId, k -> new ArrayList<>()).add(item.getIngredientNote().trim());
            }
        }

        // 3. Auto-Promote Engine
        List<RecipeStepIngredient> finalShoppingList = new ArrayList<>();

        for (Map.Entry<Integer, BigDecimal> entry : ingredientBaseSums.entrySet()) {
            Integer ingId = entry.getKey();
            BigDecimal totalBaseAmount = entry.getValue();
            Measurement originalUnit = referenceMeasurements.get(ingId);

            Integer targetBaseTypeId = originalUnit.getBaseType().getBaseTypeId();
            boolean isOriginalMetric = originalUnit.getIsMetric() != null ? originalUnit.getIsMetric() : false;

            Measurement optimizedTargetUnit = originalUnit;
            BigDecimal scaledFinalAmount = totalBaseAmount;

            // Rule: Only attempt unit promotion if dealing with Volume(1) or Weight(2),
            // AND only if the original unit allows promotion from the start
            if ((targetBaseTypeId.equals(1) || targetBaseTypeId.equals(2)) && originalUnit.getIsPromotable()) {

                List<Measurement> candidateUnits = allMeasurementsPool.stream()
                        .filter(m -> m.getBaseType().getBaseTypeId().equals(targetBaseTypeId))
                        .filter(m -> {
                            // Enforce strict lane separation: candidate system must match original system
                            // exactly
                            boolean isCandidateMetric = m.getIsMetric() != null ? m.getIsMetric() : false;
                            return m.getIsPromotable() && (isCandidateMetric == isOriginalMetric);
                        })
                        // Sort descending by scaling_rank: highest containers checked first
                        .sorted((m1, m2) -> Short.compare(m2.getScalingRank(), m1.getScalingRank()))
                        .toList();

                for (Measurement candidate : candidateUnits) {
                    String unitText = candidate.getUnitName().toLowerCase().trim();

                    // Guardrails: Prevents premature jumping to larger units
                    if (unitText.equals("qt") && totalBaseAmount.compareTo(BigDecimal.valueOf(946.352946)) < 0)
                        continue;
                    if (unitText.equals("gal") && totalBaseAmount.compareTo(BigDecimal.valueOf(3785.411784)) < 0)
                        continue;

                    BigDecimal threshold = candidate.getBaseUnitEquivalent().multiply(BigDecimal.valueOf(0.85));

                    if (totalBaseAmount.compareTo(threshold) >= 0) {
                        optimizedTargetUnit = candidate;
                        scaledFinalAmount = totalBaseAmount.divide(candidate.getBaseUnitEquivalent(), 4,
                                RoundingMode.HALF_UP);
                        break;
                    }
                }
            } else {
                // Non-promotable units (ml, l, pt) or Counts/Text scale numbers linearly but
                // freeze the unit label!
                optimizedTargetUnit = originalUnit;
                scaledFinalAmount = totalBaseAmount.divide(originalUnit.getBaseUnitEquivalent(), 4,
                        RoundingMode.HALF_UP);
            }

            // Build out the final clean row mapping object
            RecipeStepIngredient shoppingItem = new RecipeStepIngredient();
            RecipeStepIngredient sample = flatIngredients.stream()
                    .filter(i -> i.getIngredient().getIngredientId().equals(ingId))
                    .findFirst()
                    .orElse(flatIngredients.get(0));

            shoppingItem.setIngredient(sample.getIngredient());
            shoppingItem.setMeasurement(optimizedTargetUnit);

            // Type safety check to prevent compilation casting crashes
            if (sample.getAmount() instanceof BigDecimal) {
                shoppingItem.setAmount(scaledFinalAmount);
            } else {
                shoppingItem.setAmount(BigDecimal.valueOf(scaledFinalAmount.doubleValue()));
            }

            finalShoppingList.add(shoppingItem);
        }

        // 4. Group maps alphabetically A-Z by aisle category
        return finalShoppingList.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getIngredient().getGroceryCategory().getCategoryName(),
                        TreeMap::new,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    list.sort(Comparator
                                            .comparing(item -> item.getIngredient().getIngredientName().toLowerCase()));
                                    return list;
                                })));
    }

}
