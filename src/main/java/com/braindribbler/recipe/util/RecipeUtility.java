package com.braindribbler.recipe.util;

import com.braindribbler.recipe.domain.recipe.GroceryCategory;
import com.braindribbler.recipe.domain.recipe.IngredientClassification;
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
        BigDecimal decimalPartBd = amount.subtract(BigDecimal.valueOf(wholePart));

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
     * Groups ingredients strictly by their Grocery Category aisle without combining 
     * quantities or losing individual step notes. (Solves the layout visibility bug!)
     */
    public static Map<String, List<RecipeStepIngredient>> groupIngredientsByGroceryCategory(List<Recipe> recipes) {
        if (recipes == null || recipes.isEmpty()) {
            return Collections.emptyMap();
        }

        List<RecipeStepIngredient> flatIngredients = recipes.stream()
                .filter(Objects::nonNull)
                .flatMap(r -> r.getSteps().stream())
                .filter(step -> step.getStepIngredients() != null)
                .flatMap(step -> step.getStepIngredients().stream())
                .filter(item -> item.getIngredient() != null && item.getMeasurement() != null)
                .toList();

        Map<GroceryCategory, List<RecipeStepIngredient>> groupedByCategoryEntity = flatIngredients.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getIngredient().getGroceryCategory(),
                        Collectors.toList()));

        return groupedByCategoryEntity.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> entry.getKey().getDisplayOrder()))
                .collect(Collectors.toMap(
                        entry -> entry.getKey().getCategoryName(),
                        entry -> {
                            List<RecipeStepIngredient> aisleIngredients = new ArrayList<>(entry.getValue());
                            aisleIngredients.sort(Comparator.comparing(
                                    item -> item.getIngredient().getIngredientName().toLowerCase()));
                            return aisleIngredients;
                        },
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new
                ));
    }

        /**
     * Aggregates, normalizes, and auto-promotes multi-recipe ingredient batches
     * driven by database base_type, is_promotable, is_metric, and classification rules.
     */
    public static Map<String, List<RecipeStepIngredient>> aggregateAndPromoteShoppingList(
            List<Recipe> recipes,
            List<Measurement> allMeasurementsPool) {

        if (recipes == null || recipes.isEmpty()) {
            return Collections.emptyMap();
        }

        List<RecipeStepIngredient> flatIngredients = recipes.stream()
                .filter(Objects::nonNull)
                .flatMap(r -> r.getSteps().stream())
                .filter(step -> step.getStepIngredients() != null)
                .flatMap(step -> step.getStepIngredients().stream())
                .filter(item -> item.getIngredient() != null && item.getMeasurement() != null)
                .toList();

        Map<String, BigDecimal> ingredientBaseSums = new HashMap<>();
        Map<String, Measurement> referenceMeasurements = new HashMap<>();
        Map<String, List<String>> accumulatedNotes = new HashMap<>();
        Map<String, RecipeStepIngredient> sampleItems = new HashMap<>();

        for (RecipeStepIngredient item : flatIngredients) {
            Integer ingId = item.getIngredient().getIngredientId();
            Measurement unit = item.getMeasurement();
            Integer baseTypeId = unit.getBaseType().getBaseTypeId();

            // Combine Volume (1) and Weight (2) across compatible units;
            // keep Count (3) and Text (4) separated by exact measurementId
            String bucketKey = (baseTypeId.equals(1) || baseTypeId.equals(2))
                    ? ingId + ":" + baseTypeId
                    : ingId + ":" + baseTypeId + ":" + unit.getMeasurementId();

            BigDecimal amountBd = BigDecimal.valueOf(((Number) item.getAmount()).doubleValue());
            BigDecimal baseAmount = amountBd.multiply(unit.getBaseUnitEquivalent());

            ingredientBaseSums.put(
                    bucketKey,
                    ingredientBaseSums.getOrDefault(bucketKey, BigDecimal.ZERO).add(baseAmount)
            );

            // Prefer retaining a promotable measurement as the reference unit if mixed with non-promotable (e.g., Pinch + Cup)
            Measurement existingRef = referenceMeasurements.get(bucketKey);
            if (existingRef == null || (!Boolean.TRUE.equals(existingRef.getIsPromotable()) && Boolean.TRUE.equals(unit.getIsPromotable()))) {
                referenceMeasurements.put(bucketKey, unit);
            }

            sampleItems.putIfAbsent(bucketKey, item);

            if (item.getIngredientNote() != null && !item.getIngredientNote().trim().isEmpty()) {
                accumulatedNotes.computeIfAbsent(bucketKey, k -> new ArrayList<>()).add(item.getIngredientNote().trim());
            }
        }

        List<RecipeStepIngredient> finalShoppingList = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> entry : ingredientBaseSums.entrySet()) {
            String bucketKey = entry.getKey();
            BigDecimal totalBaseAmount = entry.getValue();
            Measurement originalUnit = referenceMeasurements.get(bucketKey);
            RecipeStepIngredient sample = sampleItems.get(bucketKey);

            Integer targetBaseTypeId = originalUnit.getBaseType().getBaseTypeId();
            boolean isOriginalMetric = Boolean.TRUE.equals(originalUnit.getIsMetric());
            boolean isDryIngredient = sample.getIngredient().getClassification() == IngredientClassification.DRY;

            Measurement optimizedTargetUnit = originalUnit;
            BigDecimal scaledFinalAmount = totalBaseAmount.divide(originalUnit.getBaseUnitEquivalent(), 4, RoundingMode.HALF_UP);

            if ((targetBaseTypeId.equals(1) || targetBaseTypeId.equals(2)) && Boolean.TRUE.equals(originalUnit.getIsPromotable())) {

                List<Measurement> candidateUnits = allMeasurementsPool.stream()
                        .filter(m -> m.getBaseType().getBaseTypeId().equals(targetBaseTypeId))
                        .filter(m -> {
                            boolean isCandidateMetric = Boolean.TRUE.equals(m.getIsMetric());
                            return Boolean.TRUE.equals(m.getIsPromotable()) && (isCandidateMetric == isOriginalMetric);
                        })
                        // Prevent DRY ingredients from promoting into liquid-only volumes (Pint=6, Quart=7, Gallon=9)
                        .filter(m -> !(isDryIngredient && targetBaseTypeId.equals(1) && List.of(6, 7, 9).contains(m.getMeasurementId())))
                        .sorted((m1, m2) -> Short.compare(m2.getScalingRank(), m1.getScalingRank()))
                        .toList();

                for (Measurement candidate : candidateUnits) {
                    BigDecimal promotionFloor = (candidate.getMinPromotionFloor() != null && candidate.getMinPromotionFloor().compareTo(BigDecimal.ZERO) > 0)
                            ? candidate.getMinPromotionFloor()
                            : BigDecimal.valueOf(0.85);

                    BigDecimal threshold = candidate.getBaseUnitEquivalent().multiply(promotionFloor);

                    if (totalBaseAmount.compareTo(threshold) >= 0) {
                        optimizedTargetUnit = candidate;
                        scaledFinalAmount = totalBaseAmount.divide(candidate.getBaseUnitEquivalent(), 4, RoundingMode.HALF_UP);
                        break;
                    }
                }
            }

            RecipeStepIngredient shoppingItem = new RecipeStepIngredient();
            shoppingItem.setIngredient(sample.getIngredient());
            shoppingItem.setMeasurement(optimizedTargetUnit);

            List<String> notes = accumulatedNotes.get(bucketKey);
            if (notes != null && !notes.isEmpty()) {
                String joinedNotes = notes.stream()
                        .distinct()
                        .collect(Collectors.joining(", "));
                shoppingItem.setIngredientNote(joinedNotes);
            }

            if (sample.getAmount() instanceof BigDecimal) {
                shoppingItem.setAmount(scaledFinalAmount);
            } else {
                shoppingItem.setAmount(BigDecimal.valueOf(scaledFinalAmount.doubleValue()));
            }

            finalShoppingList.add(shoppingItem);
        }

        Map<GroceryCategory, List<RecipeStepIngredient>> groupedByCategoryEntity = finalShoppingList.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getIngredient().getGroceryCategory(),
                        Collectors.toList()));

        return groupedByCategoryEntity.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> entry.getKey().getDisplayOrder()))
                .collect(Collectors.toMap(
                        entry -> entry.getKey().getCategoryName(),
                        entry -> {
                            List<RecipeStepIngredient> aisleIngredients = entry.getValue();
                            aisleIngredients.sort(Comparator.comparing(
                                    (RecipeStepIngredient item) -> item.getIngredient().getIngredientName().toLowerCase())
                                    .thenComparingInt(item -> item.getMeasurement().getBaseType().getBaseTypeId()));
                            return aisleIngredients;
                        },
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new
                ));
    }
}
