    /**
 * Recipes Dynamic Form Processing Engine - Module 1
 * Handles setup and row addition/removal frameworks.
 */
$(document).ready(function () {
    var activeSelectDropdown = null;
    var modal = document.getElementById("quickIngredientModal");

    // Unified Change / Select2 event bridge
    $(document).on("change select2:select", ".ingredient-row select", function (e) {
        var $changedSelect = $(this);
        validateStepIngredients();

        // BUGFIX: Check specifically if the select item has our ingredient dropdown styling weights
        if ($changedSelect.hasClass("select-width-weight-large")) {
            filterMeasurementsByIngredient($changedSelect);
        }
    });


    // --- STEP CARD MANAGEMENT ---
    $("#add-step-btn").click(function () {
        var firstCard = $("#steps-container .step-card").first();
        
        // Clone the card
        var clone = firstCard.clone();
        
        clone.find(".select2-container").remove(); 
        clone.find("select").removeClass("select2-hidden-accessible").removeAttr("data-select2-id");

        // Clear values safely
        clone.find("textarea, input").val("");
        clone.find("select").prop("selectedIndex", 0);
        clone.find("input[type='hidden']").remove();
        clone.find(".ingredients-container").empty();

        $("#steps-container").append(clone);
        reindexForm();
        saveCurrentDraftToLocalStorage();
    });

    $(document).on("click", ".remove-step-btn", function () {
        $(this).closest(".step-card").remove();
        reindexForm();
        validateStepIngredients();
        saveCurrentDraftToLocalStorage();
    });

    // --- MID-TIMELINE ELEMENT INSERTION SYSTEM ---
    $(document).on("click", "#insert-step-top-btn", function (e) {
        e.preventDefault();
        var firstCard = $("#steps-container .step-card").first();
        
        var clone = firstCard.clone();
        
        clone.find(".select2-container").remove();
        clone.find("select").removeClass("select2-hidden-accessible").removeAttr("data-select2-id");

        clone.find("textarea, input").val("");
        clone.find("select").prop("selectedIndex", 0);
        clone.find("input[type='hidden']").remove();
        clone.find(".ingredients-container").empty();

        $("#steps-container").prepend(clone);
        reindexForm();
        saveCurrentDraftToLocalStorage();
    });

    $(document).on("click", ".insert-step-here-btn", function (e) {
        e.preventDefault();
        var firstCard = $("#steps-container .step-card").first();
        
        var clone = firstCard.clone();
        
        clone.find(".select2-container").remove();
        clone.find("select").removeClass("select2-hidden-accessible").removeAttr("data-select2-id");

        clone.find("textarea, input").val("");
        clone.find("select").prop("selectedIndex", 0);
        clone.find("input[type='hidden']").remove();
        clone.find(".ingredients-container").empty();

        $(this).closest(".step-card").after(clone);
        reindexForm();
        saveCurrentDraftToLocalStorage();
    });

    // --- INGREDIENT ROW MANAGEMENT ---
    $(document).off("click", ".add-ingredient-btn").on("click", ".add-ingredient-btn", function (e) {
        e.preventDefault();
        var container = $(this).closest(".step-card").find(".ingredients-container");
        var ingredientOptions = $('#ingredient-options-pool').html();
        var measurementOptions = $('#measurement-options-pool').html();

        var newRowHtml =
            '<div class="ingredient-row">' +
            '  <div class="row-lane lane-one">' +
            '    <select class="form-input select-width-weight-large" required>' + ingredientOptions + '</select>' +
            '  </div>' +
            '  <div class="row-lane lane-two">' +
            '    <div class="amount-wrapper fraction-split-grid">' +
            '      <input type="hidden" class="combined-decimal-amount" value="" />' +
            '      <input type="number" min="0" step="1" class="form-input whole-amount-input" placeholder="0" />' +
            '      <select class="form-input fraction-dropdown-select">' +
            '        <option value="0">0</option>' +
            '        <option value="0.125">1/8</option>' +
            '        <option value="0.25">1/4</option>' +
            '        <option value="0.333">1/3</option>' +
            '        <option value="0.5">1/2</option>' +
            '        <option value="0.667">2/3</option>' +
            '        <option value="0.75">3/4</option>' +
            '      </select>' +
            '    </div>' +
            '    <select class="form-input select-width-weight-medium" required>' + measurementOptions + '</select>' +
            '  </div>' +
            '  <div class="row-lane lane-three">' +
            '    <input type="text" class="form-input select-width-weight-large" placeholder="Note (e.g., melted, chilled)" />' +
            '    <button type="button" class="btn remove-ing-btn btn-square-row-dismiss">×</button>' +
            '  </div>' +
            '</div>';

        container.append($(newRowHtml));
        reindexForm();

        container.find(".ingredient-row:last-child select.select-width-weight-large").each(function () {
            initializeIngredientSelect(this);
        });
        saveCurrentDraftToLocalStorage();
    });

    $(document).on("click", ".remove-ing-btn", function () {
        var $row = $(this).closest(".ingredient-row");
        // FIXED: Using broader find selector to ensure Select2 destroys properly inside wrapper divs
        $row.find("select").first().select2("destroy");
        $row.remove();
        reindexForm();
        validateStepIngredients();
        saveCurrentDraftToLocalStorage();
    });

    // --- QUICK INGREDIENT MODAL HANDLERS ---
    $(document).on("click", ".open-quick-ing-btn", function () {
        activeSelectDropdown = $(this).closest('.ingredients-section').find(".ingredients-container .ingredient-row:last-child select").first();
        $("#quickIngredientName").val("");
        $("#quickGroceryCategoryId").val("");
        $("#quickIngredientClassification").val("SOLID");
        
        $(".class-selector-card").removeClass("active");
        $('.class-selector-card[data-value="SOLID"]').addClass("active");
        
        $("#modal-feedback-msg").hide().text("");
        if (modal) modal.showModal();
    });

    $(document).on("click", ".open-inline-edit-btn", function () {
        var $btn = $(this);
        var targetId = $btn.data("id") || $btn.closest(".ingredient-row").find("select").first().val();

        if (!targetId || targetId === "") {
            showToast("Please select an ingredient from the dropdown list before editing.", false);
            return;
        }

        $.ajax({
            url: "/ingredients/edit/" + targetId,
            type: "GET",
            headers: { "X-Requested-With": "XMLHttpRequest" },
            success: function (htmlSnippet) {
                $("#inline-edit-loading-anchor").html(htmlSnippet);
                var inlineModal = document.getElementById("inlineEditIngredientModal");
                if (inlineModal) inlineModal.showModal();
            },
            error: function () {
                showToast("Could not open the editor panel. Ensure the ingredient belongs to you.", false);
            }
        });
    });

    $(document).on("click", ".class-selector-card", function (e) {
        e.preventDefault();
        var chosenValue = $(this).data("value");
        $("#quickIngredientClassification").val(chosenValue);
        $(".class-selector-card").removeClass("active");
        $(this).addClass("active");
    });

    $("#closeQuickIngBtn").click(function () { if (modal) modal.close(); });

    // --- REAL-TIME VALIDATION ENGINE ---
    function validateStepIngredients() {
        $(".ingredient-row").removeClass("row-error-highlight");
        $(".ingredient-error-msg").remove();
        var formIsValid = true;

        $("#steps-container .step-card").each(function () {
            var seenIngredients = {};
            $(this).find(".ingredient-row").each(function () {
                var $row = $(this);
                var id = $row.find("select").eq(0).val();
                if (!id) return;

                if (seenIngredients[id]) {
                    formIsValid = false;
                    seenIngredients[id].addClass("row-error-highlight");
                    $row.addClass("row-error-highlight");
                    if ($row.next(".ingredient-error-msg").length === 0) {
                        $row.after('<span class="ingredient-error-msg">⚠️ "' + $row.find("select").eq(0).find("option:selected").text() + '" is listed multiple times. Please combine rows.</span>');
                    }
                } else { seenIngredients[id] = $row; }
            });
        });
        return formIsValid;
    }

    function validateRecipeForm(skipMetadata) {
        $(".form-input-error, .select2-selection--single").removeClass("form-input-error");
        $(".ingredient-error-msg, .field-error-msg").remove();
        var isFormValid = true;

        if (!skipMetadata) {
            if ($("#title").length && $("#title").val().trim() === "") {
                isFormValid = false; $("#title").addClass("form-input-error").after('<span class="field-error-msg">⚠️ Title required.</span>');
            }
            if ($("#servings").length && ($("#servings").val().trim() === "" || parseInt($("#servings").val()) <= 0)) {
                isFormValid = false; $("#servings").addClass("form-input-error").after('<span class="field-error-msg">⚠️ Servings must be > 0.</span>');
            }
            if ($("#recipeVisibilityId").length && !$("#recipeVisibilityId").val()) {
                isFormValid = false; $("#recipeVisibilityId").addClass("form-input-error").after('<span class="field-error-msg">⚠️ Visibility required.</span>');
            }
        }

        $("#steps-container .step-card").each(function (idx) {
            var $textarea = $(this).find("textarea");
            if (!skipMetadata && $textarea.length && $textarea.val().trim() === "") {
                isFormValid = false; $textarea.addClass("form-input-error").after('<span class="field-error-msg">⚠️ Step ' + (idx + 1) + ' text missing.</span>');
            }

            $(this).find(".ingredients-container .ingredient-row").each(function () {
                var $row = $(this);
                var empty = false;
                if (!skipMetadata) {
                    if (!$row.find("select").eq(0).val()) { $row.find(".select2-container .select2-selection--single").addClass("form-input-error"); empty = true; }
                    
                    // Validate Split Inputs: Fail validation if both Whole and Fraction values are set to zero
                    var wholeVal = parseFloat($row.find(".whole-amount-input").val()) || 0;
                    var fracVal = parseFloat($row.find(".fraction-dropdown-select").val()) || 0;
                    if (wholeVal <= 0 && fracVal <= 0) { 
                        $row.find(".whole-amount-input, .fraction-dropdown-select").addClass("form-input-error"); 
                        empty = true; 
                    }
                    
                    if (!$row.find("select.select-width-weight-medium").val()) { $row.find("select.select-width-weight-medium").addClass("form-input-error"); empty = true; }
                    if (empty && $row.next(".field-error-msg").length === 0) { isFormValid = false; $row.after('<span class="field-error-msg">⚠️ Incomplete fields.</span>'); }
                }
            });
        });
        return isFormValid && validateStepIngredients();
    }

    // --- REINDEXING LOGIC & SELECT2 RE-BINDING ---
    function reindexForm() {
        $("#steps-container .step-card").each(function (stepIdx) {
            $(this).attr("data-step-index", stepIdx).find(".step-number-label").text(stepIdx + 1);
            $(this).find(".remove-step-btn").toggle($("#steps-container .step-card").length > 1);
            $(this).find("textarea").attr("name", "steps[" + stepIdx + "].instructionText");

            $(this).find(".ingredients-container .ingredient-row").each(function (ingIdx) {
                var $row = $(this);
                $row.attr("data-ing-index", ingIdx);
                
                // Targets the ingredient selector specifically by its weight class signature
                $row.find("select.select-width-weight-large").attr("name", "steps[" + stepIdx + "].ingredients[" + ingIdx + "].ingredientId");
                
                // Direct array serialization bound correctly to hidden decimal target fields
                $row.find(".combined-decimal-amount").attr("name", "steps[" + stepIdx + "].ingredients[" + ingIdx + "].amount");
                
                // Targets the measurement unit specifically by its medium weight class name
                $row.find("select.select-width-weight-medium").attr("name", "steps[" + stepIdx + "].ingredients[" + ingIdx + "].measurementId");
                
                // Targets notes field input safely
                $row.find("input[type='text'].select-width-weight-large").attr("name", "steps[" + stepIdx + "].ingredients[" + ingIdx + "].ingredientNote");
                $row.find(".remove-ing-btn").show();
            });
        });

        // Safe re-initialization layer for select2 components
        $(".ingredient-row select.select-width-weight-large").each(function () {
            initializeIngredientSelect(this);
        });
    }


    // --- FORM SUBMISSION LAYER ---
    $(document).on("click", "#save-stay-btn, #finish-view-btn", function (e) {
        e.preventDefault();
        var clickedButtonId = $(this).attr("id");
        var saveButton = $(this);
        var originalText = saveButton.html();

        if (!validateRecipeForm(false)) {
            showToast("Validation failed. Please review missing fields.", false);
            if ($(".form-input-error").length) $('html, body').animate({ scrollTop: $(".form-input-error").first().offset().top - 100 }, 400);
            return;
        }

        saveButton.prop("disabled", true).html("Processing...");
        var $form = $("#recipeForm").length ? $("#recipeForm") : $(this).closest("form");
        var token = document.cookie.split('; ').find(row => row.startsWith('XSRF-TOKEN='))?.split('=');
        var payload = $form.serialize() + "&actionType=" + (clickedButtonId === "finish-view-btn" ? "FINISH" : "SAVE");

        $.ajax({
            url: $form.attr("action") || window.location.pathname,
            type: "POST",
            data: payload,
            beforeSend: function (xhr) { if (token) xhr.setRequestHeader("X-XSRF-TOKEN", decodeURIComponent(token[1])); },
            success: function (response) {
                saveButton.prop("disabled", false).html(originalText);
                if (response.success) {
                    clearRecipeDraft();
                    if (response.recipeId) {
                        var $idInput = $("#recipeId").length ? $("#recipeId") : $form.find("input[name='recipeId']");
                        if ($idInput.length) {
                            var isBrandNew = !$idInput.val(); $idInput.val(response.recipeId);
                            if (isBrandNew) history.replaceState({ recipeId: response.recipeId }, "", "/recipes/edit/" + response.recipeId);
                        }
                    }
                    if (clickedButtonId === "finish-view-btn" && response.redirectUrl) window.location.href = response.redirectUrl;
                    else showToast("Recipe progress saved successfully!", true);
                } else { showToast(response.message || "Failed to save progress.", false); }
            },
            error: function () { saveButton.prop("disabled", false).html(originalText); showToast("Server error saving recipe data.", false); }
        });
    });

    // --- FRACTIONS TRANSLATOR ENGINE ---
    function formatCulinaryFraction(value) {
        var num = parseFloat(value); if (isNaN(num) || num <= 0) return "";
        var firstPart = Math.floor(num); var decimalPart = Math.round((num - firstPart) * 1000) / 1000;
        if (decimalPart === 0) return "";

        var fractionText = "";
        if (decimalPart >= 0.08 && decimalPart <= 0.16) fractionText = "1/8";
        else if (decimalPart > 0.16 && decimalPart <= 0.29) fractionText = "1/4";
        else if (decimalPart > 0.29 && decimalPart <= 0.40) fractionText = "1/3";
        else if (decimalPart > 0.40 && decimalPart <= 0.58) fractionText = "1/2";
        else if (decimalPart > 0.58 && decimalPart <= 0.70) fractionText = "2/3";
        else if (decimalPart > 0.70 && decimalPart <= 0.82) fractionText = "3/4";
        else if (decimalPart > 0.82 && decimalPart <= 0.93) fractionText = "7/8";
        else if (decimalPart > 0.93) return (firstPart + 1).toString();
        return (firstPart > 0 ? firstPart + " " : "") + fractionText;
    }

    $(document).on("input.fraction change.fraction focusin.fraction", ".ingredient-row .whole-amount-input", function () {
        var str = formatCulinaryFraction($(this).val());
        if (str) { $(this).closest(".amount-wrapper").find(".fraction-bubble").text("🍳 Kitchen Fraction: " + str).parent().addClass("show-bubble"); }
        else { $(this).closest(".amount-wrapper").removeClass("show-bubble"); }
    });

    $(document).on("focusout.fraction", ".ingredient-row .whole-amount-input", function () { $(this).closest(".amount-wrapper").removeClass("show-bubble"); });


    function showToast(message, isSuccess) {
        var $toast = $("#form-toast-notification");
        $toast.css("background-color", isSuccess ? "#28a745" : "#dc3545");
        $("#toast-icon").text(isSuccess ? "💾" : "⚠️"); $("#toast-message").text(message);
        $toast.css({ "transform": "translateY(0)", "opacity": "1", "pointer-events": "auto" });
        setTimeout(function () { $toast.css({ "transform": "translateY(-20px)", "opacity": "0", "pointer-events": "none" }); }, 3500);
    }

    // --- DYNAMIC UNIT FILTERING ENGINE ---
    function filterMeasurementsByIngredient($ingredientSelect) {
        var $measurementSelect = $ingredientSelect.closest(".ingredient-row").find("select.select-width-weight-medium");
        
        var classification = $ingredientSelect.find("option:selected").attr("data-classification");
        if (!classification) { $measurementSelect.html($('#measurement-options-pool').html()); return; }

        var currentUnitValue = $measurementSelect.val(); var validOptionsArray = [];
        var $masterOptions = $('<div>').append($('#measurement-options-pool').html()).find('option');

        $masterOptions.each(function () {
            var $opt = $(this); var idAttr = $opt.attr('data-unit-id'); if (!idAttr) return;
            var baseType = parseInt($opt.attr('data-base-type')); var unitId = parseInt(idAttr);
            var isMetric = $opt.attr('data-is-metric') === 'true'; var matchesTrack = false;

            if (classification === "SOLID") matchesTrack = (baseType === 2 || baseType === 3 || (baseType === 4 && unitId !== 19));
            else if (classification === "WET") matchesTrack = (baseType === 1 || baseType === 4);
            else if (classification === "DRY") matchesTrack = ((baseType === 1 && [18, 1, 2, 3, 5].includes(unitId)) || baseType === 2 || (baseType === 4 && unitId !== 19));

            if (matchesTrack && (baseType === 1 || baseType === 2) && isMetric) matchesTrack = false;
            if (matchesTrack) validOptionsArray.push($opt);
        });

        validOptionsArray.sort((a, b) => (parseInt($(a).attr('data-scaling-rank')) || 0) - (parseInt($(b).attr('data-scaling-rank')) || 0));
        var $outputHTML = $('<select>').append('<option value="">-- Unit --</option>');
        $.each(validOptionsArray, function (i, $opt) { $outputHTML.append($opt); });
        $measurementSelect.html($outputHTML.html()).val($measurementSelect.find('option[value="' + currentUnitValue + '"]').length > 0 ? currentUnitValue : "");
    }

    function filterExistingSavedRowsOnLoad() {
        $(".ingredient-row select:first-child").each(function () { if ($(this).val()) filterMeasurementsByIngredient($(this)); });
    }

    // --- DELETION SAFETY ENGINE ---
    $(document).on("submit", ".delete-recipe-form", function (e) {
        var $form = $(this); 
        var $btn = $('button[form="' + ($form.attr("id") || "deleteForm") + '"]').length ? 
                   $('button[form="' + ($form.attr("id") || "deleteForm") + '"]') : 
                   $(".inline-delete-btn");
                   
        if ($btn.length && $btn.attr("data-confirmed") === "false") {
            e.preventDefault();
            
            var originalText = $btn.html();
            $btn.data("original-label", originalText);
            
            $btn.attr("data-confirmed", "true")
                 .html("Are you sure? This cannot be recovered!")
                 .addClass("btn-danger-critical");
                 
            setTimeout(function () { 
                if ($btn.attr("data-confirmed") === "true") {
                    var savedText = $btn.data("original-label");
                    
                    $btn.attr("data-confirmed", "false")
                         .html(savedText)
                         .removeClass("btn-danger-critical"); 
                }
            }, 5000);
            return false;
        }
        return true;
    });

    // --- LIFECYCLE INITIALIZATION BOOT ---
    reindexForm();
    filterExistingSavedRowsOnLoad();

    /**
     * Global Custom Select2 Instantiation Wrapper with Rainbow Flash Callback Hook
     */
    function initializeIngredientSelect(selectElement) {
        var $select = $(selectElement);
        if ($select.hasClass("select2-hidden-accessible")) return;

        $select.select2({
            placeholder: "-- Choose Ingredient --",
            width: "resolve",
            dropdownAutoWidth: true,
            language: {
                noResults: function () {
                    var $missingBtn = $select.closest('.ingredients-section').find('.open-quick-ing-btn');
                    if (!$missingBtn.hasClass('animate-rainbow-trigger')) {
                        $missingBtn.addClass('animate-rainbow-trigger');
                        setTimeout(function () { $missingBtn.removeClass('animate-rainbow-trigger'); }, 1500);
                    }
                    return "No matches found.";
                }
            }
        });
    }

    // --- LOCAL STORAGE AUTOSAVE SYSTEM ---
    var STORAGE_KEY = "recipe_draft_autosave";

    // 1. The Autosave Listener: Debounced to save 1 second after typing stops
    var autosaveTimeout;
    $(document).on("input change select2:select", "#recipeForm input, #recipeForm textarea, #recipeForm select", function() {
        if ($(this).attr('id') === 'recipeId' || $(this).attr('name') === 'actionType') return;

        clearTimeout(autosaveTimeout);
        autosaveTimeout = setTimeout(function() {
            saveCurrentDraftToLocalStorage();
        }, 1000); // 1-second delay
    });

    // --- UNIFIED AUTOSAVE ENGINE FUNCTION ---
    function saveCurrentDraftToLocalStorage() {
        // Skip tracking fields if it's the ID itself or hidden action flags
        if ($("#recipeId").val() === 'recipeId') return;

        var formData = $("#recipeForm").serializeArray();
        
        // Track structural layout parameters to help rebuild rows accurately on load
        var structuralData = [];
        $("#steps-container .step-card").each(function(stepIdx) {
            var ingCount = $(this).find(".ingredients-container .ingredient-row").length;
            structuralData.push({ stepIndex: stepIdx, ingredientCount: ingCount });
        });

        var savePayload = {
            formData: formData,
            structure: structuralData,
            timestamp: new Date().getTime()
        };

        localStorage.setItem("recipe_draft_autosave", JSON.stringify(savePayload));
    }


    // 2. The Hydration Logic: Rebuilds DOM nodes and populates saved values
    function restoreRecipeDraft(savedData) {
        if (!savedData || !savedData.structure) return;

        // Clear existing default template steps completely
        $("#steps-container").empty();

        // Loop through the tracked structure and spawn empty containers matching the draft
        $.each(savedData.structure, function(i, stepStruct) {
            // Re-trigger your click logic programmatically to build clean step card clones
            var firstCardPool = $("#steps-container .step-card").length === 0;
            if (firstCardPool) {
                // If container is empty, temporarily drop in master pool template step to clone from
                var fallbackOptions = $('#ingredient-options-pool').html();
                var fallbackUnits = $('#measurement-options-pool').html();
                var initialStep = $('<div class="step-card form-group template-step-card-blueprint">' +
                    '<div class="step-card-header-bar"><h3 class="step-card-main-title">Step <span class="step-number-label">1</span></h3><button type="button" class="btn remove-step-btn btn-danger-action-pill">Remove Step</button></div>' +
                    '<div class="form-group"><label class="form-label">Instructions</label><textarea class="form-input" rows="2" required></textarea></div>' +
                    '<div class="ingredients-section timeline-ingredients-scope-box"><div class="ingredients-section-header-alignment"><label class="form-label label-inline-reset">Ingredients Needed for this Step</label><button type="button" class="open-quick-ing-btn embedded-modal-trigger-link">✨ Missing an ingredient?</button></div><div class="ingredients-container"></div><button type="button" class="btn add-ingredient-btn btn-high-visibility-accent">➕ Add Ingredient to Step</button></div>' +
                    '<div class="timeline-step-insertion-point-wrapper"><button type="button" class="btn insert-step-here-btn btn-dashed-insertion-action">➕ Insert a New Step</button></div></div>');
                $("#steps-container").append(initialStep);
            }

            // Target your standard add-step button logic
            if (i > 0) {
                var firstCard = $("#steps-container .step-card").first();
                var stepClone = firstCard.clone();
                stepClone.find("textarea, input").val("");
                stepClone.find("select").prop("selectedIndex", 0);
                stepClone.find(".ingredients-container").empty();
                $("#steps-container").append(stepClone);
            }

            // Spawn ingredient rows inside this step
            var currentStepCard = $("#steps-container .step-card").eq(i);
            var ingContainer = currentStepCard.find(".ingredients-container");
            var ingredientOptions = $('#ingredient-options-pool').html();
            var measurementOptions = $('#measurement-options-pool').html();

            for (var j = 0; j < stepStruct.ingredientCount; j++) {
                var newRowHtml = 
                    '<div class="ingredient-row">' +
                    '  <div class="row-lane lane-one">' +
                    '    <select class="form-input select-width-weight-large" required>' + ingredientOptions + '</select>' +
                    '  </div>' +
                    '  <div class="row-lane lane-two">' +
                    '    <div class="amount-wrapper fraction-split-grid">' +
                    '      <input type="hidden" class="combined-decimal-amount" value="" />' +
                    '      <input type="number" min="0" step="1" class="form-input whole-amount-input" placeholder="0" />' +
                    '      <select class="form-input fraction-dropdown-select">' +
                    '        <option value="0">0</option>' +
                    '        <option value="0.125">1/8</option>' +
                    '        <option value="0.25">1/4</option>' +
                    '        <option value="0.333">1/3</option>' +
                    '        <option value="0.5">1/2</option>' +
                    '        <option value="0.667">2/3</option>' +
                    '        <option value="0.75">3/4</option>' +
                    '      </select>' +
                    '    </div>' +
                    '    <select class="form-input select-width-weight-medium" required>' + measurementOptions + '</select>' +
                    '  </div>' +
                    '  <div class="row-lane lane-three">' +
                    '    <input type="text" class="form-input select-width-weight-large" placeholder="Note (e.g., melted, chilled)" />' +
                    '    <button type="button" class="btn remove-ing-btn btn-square-row-dismiss">×</button>' +
                    '  </div>' +
                    '</div>';
                ingContainer.append($(newRowHtml));
            }
        });

        // Reindex input names so they match arrays sequentially before assigning data
        reindexForm();

        // Populate fields with the matching key value strings
        $.each(savedData.formData, function(i, field) {
            if (field.name === "recipeId" || field.name === "actionType") return; // Skip unique system tokens
            
            var $input = $('[name="' + field.name + '"]');
            
            if ($input.is("select")) {
                $input.val(field.value).trigger('change.select2');
            } 
            // NEW LOGIC LAYER: Intercept the combined amount fields being reloaded
            else if ($input.hasClass("combined-decimal-amount")) {
                // Set the actual backend hidden field value
                $input.val(field.value);
                
                var totalAmount = parseFloat(field.value) || 0;
                var wholePart = Math.floor(totalAmount);
                var decimalPart = Math.round((totalAmount - wholePart) * 1000) / 1000;
                
                // Find adjacent wrapper fields
                var $wrapper = $input.closest(".amount-wrapper");
                $wrapper.find(".whole-amount-input").val(wholePart);
                
                // Fallback fuzzy selection matching target decimal patterns
                var targetedOption = "0";
                if (decimalPart >= 0.08 && decimalPart <= 0.18) targetedOption = "0.125";
                else if (decimalPart > 0.18 && decimalPart <= 0.29) targetedOption = "0.25";
                else if (decimalPart > 0.29 && decimalPart <= 0.42) targetedOption = "0.333";
                else if (decimalPart > 0.42 && decimalPart <= 0.58) targetedOption = "0.5";
                else if (decimalPart > 0.58 && decimalPart <= 0.71) targetedOption = "0.667";
                else if (decimalPart > 0.71 && decimalPart <= 0.90) targetedOption = "0.75";
                else if (decimalPart > 0.90) {
                    // Edge case handling calculation: Round up if total overflowed closer to next unit
                    $wrapper.find(".whole-amount-input").val(wholePart + 1);
                    targetedOption = "0";
                }
                
                $wrapper.find(".fraction-dropdown-select").val(targetedOption);
            } 
            else {
                $input.val(field.value);
            }
        });

        filterExistingSavedRowsOnLoad();
        showToast("Draft restored successfully!", true);
    }

    // 3. Clear storage completely when database writes return successful codes
    function clearRecipeDraft() {
        localStorage.removeItem(STORAGE_KEY);
        $("#draft-restore-alert-banner").slideUp();
    }

    // --- 4. Initial Check Boot: Detect draft on page reload ---
        // --- INITIAL DRAFT MONITOR INITIALIZER ---
    var rawDraft = localStorage.getItem(STORAGE_KEY);
    if (rawDraft) {
        var parsedDraft = JSON.parse(rawDraft);
        
        var currentFormId = $("#recipeId").val() || "";
        var draftFormId = "";
        
        // Find the recipeId parameter hidden inside the serialized formData payload
        $.each(parsedDraft.formData, function(i, field) {
            if (field.name === "recipeId") {
                draftFormId = field.value || "";
            }
        });

        if (currentFormId === draftFormId) {
            var hasSubstantiveContent = false;
            var totalIngredientsCount = 0;

            if (parsedDraft.structure) {
                $.each(parsedDraft.structure, function(i, stepStruct) {
                    totalIngredientsCount += (stepStruct.ingredientCount || 0);
                });
            }

            $.each(parsedDraft.formData, function(i, field) {
                if (field.value && field.value.trim() !== "") {
                    if (field.name === "title" || field.name === "description" || field.name.includes("instructionText")) {
                        hasSubstantiveContent = true;
                    }
                }
            });

            // Show the banner if there's real modified content waiting
            if (hasSubstantiveContent || totalIngredientsCount > 0) {
                var dateString = new Date(parsedDraft.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
                $("#draft-time-placeholder").text(dateString);
                $("#draft-restore-alert-banner").css("display", "flex").hide().fadeIn();

                $(document).on("click", "#confirm-restore-btn", function() {
                    restoreRecipeDraft(parsedDraft);
                    $("#draft-restore-alert-banner").slideUp();
                });

                $(document).on("click", "#dismiss-restore-btn", function() {
                    clearRecipeDraft();
                });
            } else {
                localStorage.removeItem(STORAGE_KEY);
            }
        } else {
            // If the draft belongs to a completely different recipe, leave it alone (or wipe it if you want a clean slate)
            console.log("Draft ignored: belongs to a different recipe session.");
        }
    }

    // --- REALTIME FRACTION TO DECIMAL SYNCHRONIZATION ENGINE ---
    var typingSplitTimeout;
    // 1. Handle Whole Input changes on keyup/input with a debounced typing delay
    $(document).on("input keyup", ".whole-amount-input", function () {
        var $wrapper = $(this).closest(".amount-wrapper");
        var $wholeInput = $(this);
        var $fracSelect = $wrapper.find(".fraction-dropdown-select");
        
        // Clear the previous timer on every single keystroke
        clearTimeout(typingSplitTimeout);

        // Standard ongoing calculation happens immediately so data-binding updates instantly
        calculateCombinedTotal($wrapper);

        // Set a 750ms delay window. If the user stops typing, parse the decimal split!
        typingSplitTimeout = setTimeout(function () {
            var rawWholeVal = $wholeInput.val();
            
            if (rawWholeVal.indexOf('.') !== -1) {
                var combinedNum = parseFloat(rawWholeVal) || 0;
                var parsedWhole = Math.floor(combinedNum);
                var decimalRemainder = Math.round((combinedNum - parsedWhole) * 1000) / 1000;
                
                // Re-assign just the integer back into the whole box input channel safely
                $wholeInput.val(parsedWhole !== 0 ? parsedWhole : "");
                
                var targetedOption = "0";
                if (decimalRemainder >= 0.05 && decimalRemainder <= 0.18) targetedOption = "0.125";
                else if (decimalRemainder > 0.18 && decimalRemainder <= 0.29) targetedOption = "0.25";
                else if (decimalRemainder > 0.29 && decimalRemainder <= 0.42) targetedOption = "0.333";
                else if (decimalRemainder > 0.42 && decimalRemainder <= 0.58) targetedOption = "0.5";
                else if (decimalRemainder > 0.58 && decimalRemainder <= 0.71) targetedOption = "0.667";
                else if (decimalRemainder > 0.71 && decimalRemainder <= 0.92) targetedOption = "0.75";
                else if (decimalRemainder > 0.92) {
                    $wholeInput.val(parsedWhole + 1);
                    targetedOption = "0";
                }
                
                $fracSelect.val(targetedOption);
                
                // Recalculate once more to bind the clean, split total back down to the model
                calculateCombinedTotal($wrapper);
            }
        }, 750); // 750 milliseconds pause threshold
    });

    // 2. Handle Fraction Selection updates instantly when flipped
    $(document).on("change", ".fraction-dropdown-select", function () {
        var $wrapper = $(this).closest(".amount-wrapper");
        calculateCombinedTotal($wrapper);
    });

    // Clean calculation utility helper
    function calculateCombinedTotal($wrapper) {
        var finalWhole = parseFloat($wrapper.find(".whole-amount-input").val()) || 0;
        var finalFraction = parseFloat($wrapper.find(".fraction-dropdown-select").val()) || 0;
        
        var combinedTotal = (finalWhole + finalFraction).toFixed(3);
        $wrapper.find(".combined-decimal-amount").val(combinedTotal).trigger('change');
    }

        // --- QUICK INGREDIENT AJAX SUBMISSION ENGINE (Form-Urlencoded Version) ---
    $("#quickIngredientForm").on("submit", function (e) {
        e.preventDefault(); // Stop native HTML dialog close/reset behavior

        var submitButton = $(this).find("button[type='submit']");
        var originalText = submitButton.html();
        submitButton.prop("disabled", true).html("Adding...");

        // Map the inputs to match your Java @RequestParam bindings ("name", "groceryCategoryId", "classification")
        var payload = {
            name: $("#quickIngredientName").val(),
            groceryCategoryId: $("#quickGroceryCategoryId").val(),
            classification: $("#quickIngredientClassification").val()
        };

        // Extract XSRF/CSRF Token security layer if present
        var token = document.cookie.split('; ').find(row => row.startsWith('XSRF-TOKEN='))?.split('=');

        $.ajax({
            url: "/ingredients/quick-add",
            type: "POST",
            // Remove application/json content type to allow default application/x-www-form-urlencoded
            data: payload, 
            beforeSend: function (xhr) { 
                if (token) xhr.setRequestHeader("X-XSRF-TOKEN", decodeURIComponent(token[1])); 
            },
            success: function (response) {
                submitButton.prop("disabled", false).html(originalText);
                
                // Checks for the "success" flag returned by your Spring Map.of response
                if (response.success) {
                    var newId = response.ingredientId;
                    var newName = response.ingredientName;
                    var newClass = payload.classification; // Retain classification weight state

                    // 1. Inject the brand new entity option into our master HTML pool template
                    var newOptionHtml = `<option value="${newId}" data-classification="${newClass}">${newName}</option>`;
                    $("#ingredient-options-pool").append(newOptionHtml);

                    // 2. Also inject it into any active select lists currently drawn on screen
                    $("select.select-width-weight-large").each(function() {
                        $(this).append(newOptionHtml);
                    });

                    // 3. Clear modal inputs, close dialog box window, and fire user success toast
                    $("#quickIngredientName").val("");
                    $("#quickGroceryCategoryId").prop("selectedIndex", 0);
                    
                    var modalEl = document.getElementById("quickIngredientModal");
                    if (modalEl) modalEl.close();
                    
                    showToast(`"${newName}" added successfully to selections!`, true);
                } else {
                    $("#modal-feedback-msg").text(response.message || "Failed to create ingredient.").show();
                }
            },
            error: function (xhr) {
                submitButton.prop("disabled", false).html(originalText);
                var errorText = "Server error while adding ingredient.";
                if (xhr.responseJSON && xhr.responseJSON.message) {
                    errorText = xhr.responseJSON.message;
                }
                $("#modal-feedback-msg").text(errorText).show();
            }
        });
    });

});