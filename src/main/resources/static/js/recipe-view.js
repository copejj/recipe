/**
 * Recipe Profile Viewer View Mode Orchestrator
 * Manages tab toggling, metric conversion triggers, and data-driven unit promotion.
 */
$(document).ready(function () {

    // Global tracking parameter for active display system lane
    var currentActiveSystem = "imperial";

    function toTitleCase(str) {
        if (!str) return "";
        return str.replace(/\w\S*/g, function (txt) {
            return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
        });
    }

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

            // Pull clean metadata attributes directly from HTML data attributes
            var baseAmount = parseFloat($amountLabel.data("base-amount"));
            var baseTypeId = parseInt($amountLabel.data("base-type-id"));
            var baseUnitEquivalent = parseFloat($amountLabel.data("base-unit-equivalent"));
            var originalUnitId = parseInt($amountLabel.data("original-unit-id"));
            var isPromotable = $amountLabel.data("is-promotable") === true;
            var classification = ($amountLabel.data("ingredient-classification") || "SOLID").toUpperCase().trim();

            if (isNaN(baseAmount) || isNaN(baseTypeId) || isNaN(baseUnitEquivalent)) return;

            var totalScaledValue = baseAmount * scalingFactor;
            var absoluteBaseUnitsSum = totalScaledValue * baseUnitEquivalent;

            // 1. Grab the existing text from the HTML element to use as the absolute fallback
            var nativeDatabaseUnitText = $unitLabel.length ? ($unitLabel.data("original-unit-name") || $unitLabel.text().trim()) : "Units";

            // 2. Direct ID match to find the initial configuration inside the matrix array
            var originalUnitMeta = poolMatrix.find(function (m) { return m.measurementId === originalUnitId; });

            var finalTargetUnitText = originalUnitMeta ? originalUnitMeta.unitName : nativeDatabaseUnitText;
            var finalDisplayQuantity = totalScaledValue;

            // Only process promotions for volumetric liquids (1) and masses (2)
            if (baseTypeId === 1 || baseTypeId === 2) {

                var candidates = poolMatrix.filter(function (m) {
                    return m.baseTypeId === baseTypeId && m.isMetric === targetMetricFlag;
                });

                // Dry backing tracks ignore specific larger fluid items
                if (baseTypeId === 1 && classification === "DRY") {
                    var restrictedIds = [4, 6, 7, 9]; // fl oz, pt, qt, gal
                    candidates = candidates.filter(function (m) {
                        return !restrictedIds.includes(m.measurementId);
                    });
                }

                // Sort largest unit rank descending
                candidates.sort(function (a, b) { return b.scalingRank - a.scalingRank; });

                // Find baseline fallback matching system
                var baseUnitFallback = candidates.find(function (m) { return m.scalingRank === 1; });
                if (baseUnitFallback) {
                    finalTargetUnitText = baseUnitFallback.unitName;
                    finalDisplayQuantity = absoluteBaseUnitsSum / parseFloat(baseUnitFallback.baseUnitEquivalent);
                }

                // Check for dynamic size upgrades if unit is promotable
                if (isPromotable) {
                    for (var i = 0; i < candidates.length; i++) {
                        var candidate = candidates[i];
                        var equivValue = parseFloat(candidate.baseUnitEquivalent);

                        if (absoluteBaseUnitsSum >= (equivValue * 0.85)) {
                            if (candidate.isPromotable || candidate.measurementId === originalUnitId) {
                                finalTargetUnitText = candidate.unitName;
                                finalDisplayQuantity = absoluteBaseUnitsSum / equivValue;
                                break;
                            }
                        }
                    }
                }
            }

            // Output clean, naturally cased values directly from your database records
            if (targetMetricFlag && finalDisplayQuantity >= 1) {
                $amountLabel.text(Math.round(finalDisplayQuantity * 10) / 10);
            } else {
                $amountLabel.text(formatToKitchenFraction(finalDisplayQuantity));
            }

            if ($unitLabel.length) {
                // --- DATABASE-DRIVEN PLURALIZATION ENGINE ---
                if ($unitLabel.length) {
                    var displayQuantityNumeric = finalDisplayQuantity;
                    var finalUnitTextOutput = finalTargetUnitText;

                    // Look up the matching unit in the matrix pool using the text signature or ID
                    var matchedMatrixUnit = poolMatrix.find(function (m) {
                        return m.unitName === finalTargetUnitText || m.pluralName === finalTargetUnitText;
                    });

                    if (matchedMatrixUnit) {
                        // Dynamically toggle names based on the numeric value
                        if (displayQuantityNumeric === 1) {
                            finalUnitTextOutput = matchedMatrixUnit.unitName; // e.g., "Pint"
                        } else {
                            // Fall back to standard unitName if pluralName happens to be blank/null
                            finalUnitTextOutput = matchedMatrixUnit.pluralName || matchedMatrixUnit.unitName; // e.g., "Pints"
                        }
                    }

                    // Hide the word "Whole" completely from layouts (e.g., "84 Whole Green Beans" -> "84 Green Beans")
                    if (finalUnitTextOutput.toLowerCase() === "whole") {
                        $unitLabel.text("");
                    } else {
                        $unitLabel.text(finalUnitTextOutput + " ");
                    }
                }
            }

            // Ounce Packaging Preview Display Engine
            if ($ozPreviewLabel.length) {
                if ((baseTypeId === 1 || baseTypeId === 2) && !targetMetricFlag) {
                    var divFactor = (baseTypeId === 1 && classification === "DRY") || baseTypeId === 2 ? 28.349523 : 29.573530;
                    var suffix = (baseTypeId === 1 && classification !== "DRY") ? " FL OZ" : " OZ";
                    var cleanOz = Math.round((absoluteBaseUnitsSum / divFactor) * 10) / 10;
                    $ozPreviewLabel.text(" (" + cleanOz + suffix + ")");
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
