/**
 * Recipe Profile Viewer View Mode Orchestrator
 * Manages tab toggling, metric conversion triggers, and delegates unit math to RecipeScalingEngine.
 */
$(document).ready(function () {
    // --- READ PAGE METADATA FROM DOM ---
    const metaEl = document.getElementById("recipe-page-meta");
    const RECIPE_ID = metaEl ? Number(metaEl.dataset.recipeId) : 0;
    const RECIPE_IS_OWNER = metaEl ? metaEl.dataset.isOwner === "true" : false;
    const RECIPE_VISIBILITY_ID = metaEl ? Number(metaEl.dataset.visibilityId) : 1;
    const MEASUREMENT_MATRIX = window.RecipeScalingEngine
        ? window.RecipeScalingEngine.getMeasurementMatrix()
        : [];

    // --- LOCAL STORAGE STATE PERSISTENCE ---
    var storageKey = RECIPE_ID ? ("recipe_view_state_" + RECIPE_ID) : null;

    function saveViewState() {
        var checkedKeys = [];
        $(".shopping-item-checkbox").each(function () {
            if ($(this).is(":checked")) {
                checkedKeys.push(String($(this).data("item-key")));
            }
        });

        var state = {
            activeTab: $(".tab-btn.active").data("target") || "overview-panel",
            system: currentActiveSystem,
            servings: $("#servings-scaler").val(),
            checkedItems: checkedKeys
        };

        try {
            localStorage.setItem(storageKey, JSON.stringify(state));
        } catch (e) {
            // Ignore storage quota or private browsing errors
        }
        updateResetButtonVisibility();
    }

    function applyCheckboxVisuals($checkbox) {
        var itemRow = $checkbox.closest(".recipe-ingredient-row");
        var textWrapper = itemRow.find(".shopping-item-text");
        var noteInput = itemRow.find(".shopping-note-input");

        if ($checkbox.is(":checked")) {
            textWrapper.addClass("crossed-out-text");
            itemRow.addClass("pantry-owned-item");
            if (noteInput.length) noteInput.prop("disabled", true).css("opacity", "0.3");
        } else {
            textWrapper.removeClass("crossed-out-text");
            itemRow.removeClass("pantry-owned-item");
            if (noteInput.length) noteInput.prop("disabled", false).css("opacity", "1");
        }
    }

    function updateResetButtonVisibility() {
        var $servingsInput = $("#servings-scaler");
        var currentServings = parseFloat($servingsInput.val());
        var originalServings = parseFloat($servingsInput.data("original-servings"));
        var activeTab = $(".tab-btn.active").data("target") || "overview-panel";
        var hasCheckedItems = $(".shopping-item-checkbox:checked").length > 0;

        var isModified = (currentServings !== originalServings) ||
                        (currentActiveSystem !== "imperial") ||
                        (activeTab !== "overview-panel") ||
                        hasCheckedItems;

        var $resetPanel = $("#reset-widget-panel");

        if (isModified) {
            if (!$resetPanel.is(":visible")) {
                $resetPanel.fadeIn(200);
            }
        } else {
            if (storageKey) {
                localStorage.removeItem(storageKey);
            }

            if ($resetPanel.is(":visible")) {
                $resetPanel.fadeOut(200);
            }
        }
    }

    function restoreViewState() {
        var rawState = localStorage.getItem(storageKey);
        if (!rawState) return;

        try {
            var state = JSON.parse(rawState);

            // 1. Restore System (Imperial / Metric)
            if (state.system && (state.system === "imperial" || state.system === "metric")) {
                currentActiveSystem = state.system;
                $(".system-toggle-btn").removeClass("active").css({ "background-color": "transparent", "color": "var(--color-text-muted)" });
                $('.system-toggle-btn[data-system="' + state.system + '"]')
                    .addClass("active")
                    .css({ "background-color": "var(--color-primary)", "color": "white" });
            }

            // 2. Restore Servings
            if (state.servings && parseFloat(state.servings) > 0) {
                $("#servings-scaler").val(state.servings);
            }

            // 3. Restore Checked Shopping List Items
            if (Array.isArray(state.checkedItems)) {
                $(".shopping-item-checkbox").each(function () {
                    var $cb = $(this);
                    var key = String($cb.data("item-key"));
                    var shouldBeChecked = state.checkedItems.indexOf(key) !== -1;
                    $cb.prop("checked", shouldBeChecked);
                    applyCheckboxVisuals($cb);
                });
            }

            // 4. Restore Active Tab
            if (state.activeTab && $("#" + state.activeTab).length) {
                $(".tab-btn").removeClass("active").attr("aria-selected", "false").css({ "color": "var(--color-text-muted, #6c757d)", "border-bottom": "none", "margin-bottom": "0" });
                $('.tab-btn[data-target="' + state.activeTab + '"]')
                    .addClass("active")
                    .attr("aria-selected", "true")
                    .css({ "color": "var(--color-primary, #0d6efd)", "border-bottom": "3px solid var(--color-primary, #0d6efd)", "margin-bottom": "-13px" });

                $(".tab-panel").hide();
                $("#" + state.activeTab).show();
            }
        } catch (e) {
            localStorage.removeItem(storageKey);
        }
        updateResetButtonVisibility();
    }
    var currentActiveSystem = "imperial";

    // --- PERSPECTIVE PANEL TABS TOGGLER ---
    $(".tab-btn").click(function () {
        var clickedTab = $(this);
        var targetPanelId = clickedTab.data("target");

        $(".tab-btn")
            .removeClass("active")
            .attr("aria-selected", "false")
            .css({ "color": "var(--color-text-muted, #6c757d)", "border-bottom": "none", "margin-bottom": "0" });

        clickedTab
            .addClass("active")
            .attr("aria-selected", "true")
            .css({ "color": "var(--color-primary, #0d6efd)", "border-bottom": "3px solid var(--color-primary, #0d6efd)", "margin-bottom": "-13px" });

        saveViewState();

        $(".tab-panel").hide();
        $("#" + targetPanelId).fadeIn(150, function () {
            runLiveScalingEngine();
        });
    });

    // --- DYNAMIC SYSTEM CONVERSION MODE CONTROLLER ---
    $(".system-toggle-btn").click(function (e) {
        e.preventDefault();
        var clickedBtn = $(this);
        var selectedSystem = clickedBtn.data("system");

        if (selectedSystem === currentActiveSystem) return;
        currentActiveSystem = selectedSystem;

        $(".system-toggle-btn").removeClass("active").css({ "background-color": "transparent", "color": "var(--color-text-muted)" });
        clickedBtn.addClass("active").css({ "background-color": "var(--color-primary)", "color": "white" });

        runLiveScalingEngine();
        saveViewState();
    });

    // --- DELEGATED LIVE SCALING AND PROMOTION ENGINE ---
    function runLiveScalingEngine() {
        var inputField = $("#servings-scaler");
        var customServings = parseFloat(inputField.val());
        var originalServings = parseFloat(inputField.data("original-servings"));

        if (isNaN(customServings) || customServings <= 0 || isNaN(originalServings) || originalServings <= 0) return;

        if (window.RecipeScalingEngine) {
            window.RecipeScalingEngine.updateAll({
                scalingFactor: customServings / originalServings,
                system: currentActiveSystem,
                matrix: MEASUREMENT_MATRIX
            });
        }
    }

    // --- INTERACTIVE ELEMENT ACTIONS ---
    $("#servings-scaler").on("input change", function () {
        runLiveScalingEngine();
        saveViewState();
    });

    $(document).on("change", ".shopping-item-checkbox", function () {
        applyCheckboxVisuals($(this));
        saveViewState();
    });

    // --- SHARE RECIPE & ADDRESS-BAR TRACKING ---
    if (!RECIPE_IS_OWNER && RECIPE_VISIBILITY_ID !== 2) {
        if (!window.location.pathname.endsWith("/share")) {
            window.history.replaceState(null, "", window.location.pathname + "/share");
        }
    }

    $("#shareRecipeBtn").on("click", async function () {
        var $shareBtn = $(this);
        var shareUrl = $shareBtn.data("share-url");
        var recipeTitle = $shareBtn.data("recipe-title") || "Check out this recipe";

        if (!RECIPE_IS_OWNER) {
            fetch(shareUrl, {
                method: "POST",
                headers: window.getCsrfHeaders(),
                credentials: "same-origin"
            }).catch(function () { /* Ignore background tracking errors */ });
        }

        if (navigator.share && /Mobi|Android|iPhone|iPad/i.test(navigator.userAgent)) {
            try {
                await navigator.share({
                    title: recipeTitle,
                    url: shareUrl
                });
                return;
            } catch (err) {
                // Fall back to clipboard copy
            }
        }

        try {
            await navigator.clipboard.writeText(shareUrl);
            var originalText = $shareBtn.text();
            $shareBtn.text("Link Copied!").addClass("is-copied");

            setTimeout(function () {
                $shareBtn.text(originalText).removeClass("is-copied");
            }, 2000);
        } catch (err) {
            window.prompt("Copy this recipe link:", shareUrl);
        }
    });

    // --- RESET VIEW STATE HANDLER ---
    $("#reset-view-btn").on("click", function (e) {
        e.preventDefault();

        var $servingsInput = $("#servings-scaler");
        var originalServings = $servingsInput.data("original-servings");
        $servingsInput.val(originalServings);

        currentActiveSystem = "imperial";
        $(".system-toggle-btn").removeClass("active").css({
            "background-color": "transparent",
            "color": "var(--color-text-muted)"
        });
        $("#system-imperial-btn").addClass("active").css({
            "background-color": "var(--color-primary)",
            "color": "white"
        });

        $(".shopping-item-checkbox").each(function () {
            var $cb = $(this);
            $cb.prop("checked", false);
            applyCheckboxVisuals($cb);
        });

        $(".tab-btn").removeClass("active").attr("aria-selected", "false").css({
            "color": "var(--color-text-muted, #6c757d)",
            "border-bottom": "none",
            "margin-bottom": "0"
        });
        $('.tab-btn[data-target="overview-panel"]').addClass("active").attr("aria-selected", "true").css({
            "color": "var(--color-primary, #0d6efd)",
            "border-bottom": "3px solid var(--color-primary, #0d6efd)",
            "margin-bottom": "-13px"
        });

        $(".tab-panel").hide();
        $("#overview-panel").show();

        runLiveScalingEngine();
        updateResetButtonVisibility();
    });

    restoreViewState();

    // Apply ?servings= override from Meal Plan links (takes precedence over saved localStorage state)
    const urlParams = new URLSearchParams(window.location.search);
    const queryServings = parseFloat(urlParams.get("servings"));
    if (!isNaN(queryServings) && queryServings > 0) {
        $("#servings-scaler").val(queryServings);
        saveViewState();
        updateResetButtonVisibility();
    }

    runLiveScalingEngine();
});
