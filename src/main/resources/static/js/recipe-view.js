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
