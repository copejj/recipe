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

