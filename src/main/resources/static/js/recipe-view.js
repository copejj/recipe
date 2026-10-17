/**
 * Recipe Profile Viewer View Mode Orchestrator
 * Manages tab toggling, metric conversion triggers, and data-driven unit promotion.
 */
$(document).ready(function () {

    // Global tracking parameter for active display system lane
    var currentActiveSystem = "imperial";

    // --- PERSPECTIVE PANEL TABS TOGGLER ---
    $(".tab-btn").click(function () {
        var clickedTab = $(this);
        var targetPanelId = clickedTab.data("target");

        $(".tab-btn").removeClass("active").css({ "color": "var(--color-text-muted, #6c757d)", "border-bottom": "none", "margin-bottom": "0" });
        clickedTab.addClass("active").css({ "color": "var(--color-primary, #0d6efd)", "border-bottom": "3px solid var(--color-primary, #0d6efd)", "margin-bottom": "-13px" });

        $(".tab-panel").hide();
        $("#" + targetPanelId).fadeIn(150);
    });

    // --- DYNAMIC SYSTEM CONVERSION MODE CONTROLLER ---
    $(".system-toggle-btn").click(function (e) {
        e.preventDefault();
        var clickedBtn = $(this);
        var selectedSystem = clickedBtn.data("system");

        if (selectedSystem === currentActiveSystem) return;
        currentActiveSystem = selectedSystem;

        // Reset toggle tab button layouts
        $(".system-toggle-btn").removeClass("active").css({ "background-color": "transparent", "color": "var(--color-text-muted)" });
        clickedBtn.addClass("active").css({ "background-color": "var(--color-primary)", "color": "white" });

        // Force recalculation engine pass over layout elements
        runLiveScalingEngine();
    });

    // --- FRACTION VISUAL FORMATTER TOOL ---
    function formatToKitchenFraction(value) {
        var firstPart = Math.floor(value);
        var decimalPart = Math.round((value - firstPart) * 1000) / 1000;
        var fraction = "";

        if (decimalPart >= 0.08 && decimalPart <= 0.16) fraction = "1/8";
        else if (decimalPart > 0.16 && decimalPart <= 0.29) fraction = "1/4";
        else if (decimalPart > 0.29 && decimalPart <= 0.40) fraction = "1/3";
        else if (decimalPart > 0.40 && decimalPart <= 0.58) fraction = "1/2";
        else if (decimalPart > 0.58 && decimalPart <= 0.70) fraction = "2/3";
        else if (decimalPart > 0.70 && decimalPart <= 0.82) fraction = "3/4";
        else if (decimalPart > 0.82 && decimalPart <= 0.93) fraction = "7/8";
        else if (decimalPart > 0.93) { firstPart++; decimalPart = 0; }

        var output = (firstPart > 0 ? firstPart : "") + (fraction ? " " + fraction : "");
        return output.trim() || "0";
    }

    // --- REUSABLE LIVE SCALING AND PROMOTION ENGINE ---
    function runLiveScalingEngine() {
        var inputField = $("#servings-scaler");
        var customServings = parseFloat(inputField.val());
        var originalServings = parseFloat(inputField.data("original-servings"));

        if (isNaN(customServings) || customServings <= 0 || isNaN(originalServings) || originalServings <= 0) return;

        var scalingFactor = customServings / originalServings;
        var poolMatrix = window.MEASUREMENT_MATRIX || [];
        var targetMetricFlag = (currentActiveSystem === "metric");

        $(".scalable-amount").each(function () {
            var $amountLabel = $(this);
            var $unitLabel = $amountLabel.siblings(".scalable-unit-name");
            var $ozPreviewLabel = $amountLabel.siblings(".oz-package-preview");

            var baseAmount = parseFloat($amountLabel.data("base-amount"));
            var baseTypeId = parseInt($amountLabel.data("base-type-id"));
            var baseUnitEquivalent = parseFloat($amountLabel.data("base-unit-equivalent"));
            var originalUnitName = ($amountLabel.data("original-unit-name") || "").toLowerCase().trim();

            var classification = ($amountLabel.data("ingredient-classification") || "SOLID").toUpperCase().trim();

            if (isNaN(baseAmount) || isNaN(baseTypeId) || isNaN(baseUnitEquivalent)) return;

            var totalScaledValue = baseAmount * scalingFactor;
            var absoluteBaseUnitsSum = totalScaledValue * baseUnitEquivalent;

            // Extract the unchanging target configuration parameters from window memory lookups
            var originalMeta = poolMatrix.find(function (m) {
                return (m.unitName || "").toLowerCase().trim() === originalUnitName;
            }) || { isPromotable: true, isMetric: false };

            var finalTargetUnitText = originalUnitName;
            var finalDisplayQuantity = totalScaledValue;

            // Only attempt promotions for Volume (1) and Weight (2) buckets
            if (baseTypeId === 1 || baseTypeId === 2) {

                // DATA-DRIVEN SEPARATION FILTER: Restricts calculations to matching track flags
                var candidates = poolMatrix.filter(function (m) {
                    return m.baseTypeId === baseTypeId && m.isMetric === targetMetricFlag;
                });

                if (baseTypeId === 1 && classification === "DRY") {
                    candidates = candidates.filter(function (m) {
                        var unit = (m.unitName || "").toLowerCase().trim();
                        return unit !== "fl oz" && unit !== "fluid ounce" &&
                            unit !== "pt" && unit !== "pint" &&
                            unit !== "qt" && unit !== "quart" &&
                            unit !== "gal" && unit !== "gallon";
                    });
                }

                // Sort by rank descending
                candidates.sort(function (a, b) { return b.scalingRank - a.scalingRank; });

                // Establish base system fallback positions to keep metrics continuous
                var baseUnitFallback = candidates.find(function (m) { return m.scalingRank === 1; });
                if (baseUnitFallback) {
                    finalTargetUnitText = baseUnitFallback.unitName;
                    finalDisplayQuantity = absoluteBaseUnitsSum / parseFloat(baseUnitFallback.baseUnitEquivalent);
                }

                // Run data check loops if the original item configuration allowed promotions
                if (originalMeta.isPromotable) {
                    for (var i = 0; i < candidates.length; i++) {
                        var candidate = candidates[i];
                        var equivValue = parseFloat(candidate.baseUnitEquivalent);
                        var threshold = equivValue * 0.85;

                        if (absoluteBaseUnitsSum >= threshold) {
                            if (candidate.isPromotable || candidate.unitName.toLowerCase() === originalUnitName) {
                                finalTargetUnitText = candidate.unitName;
                                finalDisplayQuantity = absoluteBaseUnitsSum / equivValue;
                                break;
                            }
                        }
                    }
                }
            }

            // 5. Output values back to the screen display layers
            if (targetMetricFlag && finalDisplayQuantity >= 1) {
                $amountLabel.text(Math.round(finalDisplayQuantity * 10) / 10);
            } else {
                $amountLabel.text(formatToKitchenFraction(finalDisplayQuantity));
            }

            if ($unitLabel.length) {
                $unitLabel.text(finalTargetUnitText);
            }

            // Ounce Packaging Preview Display Engine
            // --- Update this block inside your runLiveScalingEngine() loop in recipe-view.js ---
            if ($ozPreviewLabel.length) {
                if (baseTypeId === 1 && !targetMetricFlag) {
                    // Read our newly integrated user-driven classification string
                    var classification = ($amountLabel.data("ingredient-classification") || "SOLID").toUpperCase().trim();

                    if (classification === "DRY") {
                        // Dry baking volume tracking (e.g. Cups of Flour) converts cleanly to mass ounces
                        var cleanOz = Math.round((absoluteBaseUnitsSum / 28.349523) * 10) / 10;
                        $ozPreviewLabel.text(" (" + cleanOz + " OZ)");
                    } else {
                        // Liquids continue to output clean fluid ounces natively
                        var cleanFlOz = Math.round((absoluteBaseUnitsSum / 29.573530) * 10) / 10;
                        $ozPreviewLabel.text(" (" + cleanFlOz + " FL OZ)");
                    }
                } else if (baseTypeId === 2 && !targetMetricFlag) {
                    // Standard dry weight measurements (e.g. Pounds/Grams)
                    var cleanOz = Math.round((absoluteBaseUnitsSum / 28.349523) * 10) / 10;
                    $ozPreviewLabel.text(" (" + cleanOz + " OZ)");
                } else {
                    $ozPreviewLabel.text("");
                }
            }
        });
    }

    // --- INTERACTIVE ELEMENT ACTIONS ---
    $("#servings-scaler").on("input change", function () {
        runLiveScalingEngine();
    });

    $(document).on("change", ".shopping-item-checkbox", function () {
        var checkbox = $(this);
        var itemRow = checkbox.closest(".shopping-item-row");
        var textWrapper = itemRow.find(".shopping-item-text");
        var noteInput = itemRow.find(".shopping-note-input");

        if (!checkbox.is(":checked")) {
            textWrapper.addClass("crossed-out-text");
            itemRow.addClass("pantry-owned-item");
            if (noteInput.length) noteInput.prop("disabled", true).css("opacity", "0.3");
        } else {
            textWrapper.removeClass("crossed-out-text");
            itemRow.removeClass("pantry-owned-item");
            if (noteInput.length) noteInput.prop("disabled", false).css("opacity", "1");
        }
    });

    // Fire calculations instantly upon initial thread presentation
    runLiveScalingEngine();
});
