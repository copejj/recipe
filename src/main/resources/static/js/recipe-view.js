/**
 * Recipe Profile Viewer View Mode Orchestrator
 * Toggles visibility between Overview, Prep Cards, and Consolidated Shopping panels.
 */
$(document).ready(function () {

    // --- LAYOUT SWITCHER NAVIGATION TAB LOGIC ---
    $(".tab-btn").click(function () {
        var clickedTab = $(this);
        var targetPanelId = clickedTab.data("target");

        // 1. Reset all tabs to standard deselect typography styles
        $(".tab-btn").removeClass("active").css({
            "color": "var(--color-text-muted, #6c757d)",
            "border-bottom": "none",
            "margin-bottom": "0"
        });

        // 2. Apply active highlight weighting to the selected tab header
        clickedTab.addClass("active").css({
            "color": "var(--color-primary, #0d6efd)",
            "border-bottom": "3px solid var(--color-primary, #0d6efd)",
            "margin-bottom": "-13px"
        });

        // 3. Hide all display panels and fade in the target viewport sheet container
        $(".tab-panel").hide();
        $("#" + targetPanelId).fadeIn(150);
    });
});

// --- SHOPPING CHECKLIST PANTRY FILTER SYSTEM ---
// Listens when a user unchecks an item they already have in their kitchen
$(document).on("change", ".shopping-item-checkbox", function () {
    var checkbox = $(this);
    var itemRow = checkbox.closest(".shopping-item-row");
    var textWrapper = itemRow.find(".shopping-item-text");

    if (!checkbox.is(":checked")) {
        // User ALREADY HAS this item: strike it out visually and flag it to vanish on print
        textWrapper.addClass("crossed-out-text");
        itemRow.addClass("pantry-owned-item");
    } else {
        // User NEEDS this item: return it to pristine printable focus states
        textWrapper.removeClass("crossed-out-text");
        itemRow.removeClass("pantry-owned-item");
    }

    // OPTIONAL EXTRA: If all items in a category section get checked off, hide the header too!
    var categorySection = checkbox.closest(".shopping-category-section");
    var visibleItems = categorySection.find(".shopping-item-row").not(".pantry-owned-item").length;
    categorySection.toggle(visibleItems > 0);
});

$(document).ready(function () {

    // --- TAB VIEW LAYER TOGGLER ---
    $(".tab-btn").click(function () {
        var clickedTab = $(this);
        var targetPanelId = clickedTab.data("target");

        $(".tab-btn").removeClass("active").css({ "color": "var(--color-text-muted, #6c757d)", "border-bottom": "none", "margin-bottom": "0" });
        clickedTab.addClass("active").css({ "color": "var(--color-primary, #0d6efd)", "border-bottom": "3px solid var(--color-primary, #0d6efd)", "margin-bottom": "-13px" });

        $(".tab-panel").hide();
        $("#" + targetPanelId).fadeIn(150);
    });

    // --- FRACTION UTILITY CLIENT FALLBACK FOR SCALER ---
    function computeLiveFraction(rawAmount) {
        var firstPart = Math.floor(rawAmount);
        var decimalPart = Math.round((rawAmount - firstPart) * 1000) / 1000;
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

    // --- DYNAMIC LIVE SCALER TRANSACTION LISTENER ---
    $("#servings-scaler").on("input change", function () {
        var inputField = $(this);
        var customServingsCount = parseFloat(inputField.val());
        var originalServingsCount = parseFloat(inputField.data("original-servings"));

        if (isNaN(customServingsCount) || customServingsCount <= 0) return;

        // Establish our scaling scale multiplier factor ratio
        var scalingMultiplierRatio = customServingsCount / originalServingsCount;

        // Scan through every registered ingredient across all active panel tabs
        $(".scalable-amount").each(function () {
            var label = $(this);
            var baselineBaseAmount = parseFloat(label.data("base-amount"));

            if (!isNaN(baselineBaseAmount)) {
                // Calculate new scaled amount parameters
                var scaledValueAmount = baselineBaseAmount * scalingMultiplierRatio;

                // Formats the value into a kitchen fraction and rewrites the visual node container text
                var fractionalKitchenText = computeLiveFraction(scaledValueAmount);
                label.text(fractionalKitchenText);
            }
        });
    });
});

