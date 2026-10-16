package com.braindribbler.recipe.util;

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

}
