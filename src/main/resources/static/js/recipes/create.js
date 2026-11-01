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

        if ($changedSelect.is("select:first-child")) {
            filterMeasurementsByIngredient($changedSelect);
        }
    });

    // --- STEP CARD MANAGEMENT ---
    $("#add-step-btn").click(function () {
        var firstCard = $("#steps-container .step-card").first();
        firstCard.find(".ingredient-row select:first-child").each(function () {
            if ($(this).hasClass("select2-hidden-accessible")) { $(this).select2("destroy"); }
        });

        var clone = firstCard.clone();
        clone.find("textarea, input").val("");
        clone.find("select").prop("selectedIndex", 0);
        clone.find("input[type='hidden']").remove();
        clone.find(".ingredients-container").empty();

        $("#steps-container").append(clone);
        reindexForm();
    });

    $(document).on("click", ".remove-step-btn", function () {
        $(this).closest(".step-card").remove();
        reindexForm();
        validateStepIngredients();
    });

    // --- MID-TIMELINE ELEMENT INSERTION SYSTEM ---
    $(document).on("click", "#insert-step-top-btn", function (e) {
        e.preventDefault();
        var firstCard = $("#steps-container .step-card").first();
        
        firstCard.find(".ingredient-row select:first-child").each(function () {
            if ($(this).hasClass("select2-hidden-accessible")) { $(this).select2("destroy"); }
        });

        var clone = firstCard.clone();
        clone.find("textarea, input").val("");
        clone.find("select").prop("selectedIndex", 0);
        clone.find("input[type='hidden']").remove();
        clone.find(".ingredients-container").empty();

        $("#steps-container").prepend(clone);
        reindexForm();
    });

    $(document).on("click", ".insert-step-here-btn", function (e) {
        e.preventDefault();
        var firstCard = $("#steps-container .step-card").first();
        
        firstCard.find(".ingredient-row select:first-child").each(function () {
            if ($(this).hasClass("select2-hidden-accessible")) { $(this).select2("destroy"); }
        });

        var clone = firstCard.clone();
        clone.find("textarea, input").val("");
        clone.find("select").prop("selectedIndex", 0);
        clone.find("input[type='hidden']").remove();
        clone.find(".ingredients-container").empty();

        $(this).closest(".step-card").after(clone);
        reindexForm();
    });

    // --- INGREDIENT ROW MANAGEMENT ---
    $(document).off("click", ".add-ingredient-btn").on("click", ".add-ingredient-btn", function (e) {
        e.preventDefault();
        var container = $(this).closest(".step-card").find(".ingredients-container");
        var ingredientOptions = $('#ingredient-options-pool').html();
        var measurementOptions = $('#measurement-options-pool').html();

        var newRowHtml =
            '<div class="ingredient-row row-flex-alignment-grid">' +
            '<select class="form-input select-width-weight-large" required>' + ingredientOptions + '</select>' +
            '<div class="amount-wrapper">' +
            '<input type="number" step="0.01" class="form-input amount-input input-forced-full-width" placeholder="Amount" required />' +
            '<div class="fraction-bubble"></div>' +
            '</div>' +
            '<select class="form-input select-width-weight-medium" required>' + measurementOptions + '</select>' +
            '<input type="text" class="form-input select-width-weight-large" placeholder="Note (e.g., melted, chilled)" />' +
            '<button type="button" class="btn remove-ing-btn btn-square-row-dismiss">×</button>' +
            '</div>';

        container.append($(newRowHtml));
        reindexForm();
    });

    $(document).on("click", ".remove-ing-btn", function () {
        var $row = $(this).closest(".ingredient-row");
        $row.find("select:first-child").select2("destroy");
        $row.remove();
        reindexForm();
        validateStepIngredients();
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
                    if (!$row.find(".amount-input").val() || parseFloat($row.find(".amount-input").val()) <= 0) { $row.find(".amount-input").addClass("form-input-error"); empty = true; }
                    if (!$row.find("select").eq(1).val()) { $row.find("select").eq(1).addClass("form-input-error"); empty = true; }
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
                $row.find("select").eq(0).attr("name", "steps[" + stepIdx + "].ingredients[" + ingIdx + "].ingredientId");
                $row.find("input[type='number']").attr("name", "steps[" + stepIdx + "].ingredients[" + ingIdx + "].amount");
                $row.find("select").eq(1).attr("name", "steps[" + stepIdx + "].ingredients[" + ingIdx + "].measurementId");
                $row.find("input[type='text']").attr("name", "steps[" + stepIdx + "].ingredients[" + ingIdx + "].ingredientNote");
                $row.find(".remove-ing-btn").show();
            });
        });

        $(".ingredient-row select:first-child").each(function () {
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

    $(document).on("input.fraction change.fraction focusin.fraction", ".ingredient-row .amount-input", function () {
        var str = formatCulinaryFraction($(this).val());
        if (str) { $(this).closest(".amount-wrapper").find(".fraction-bubble").text("🍳 Kitchen Fraction: " + str).parent().addClass("show-bubble"); }
        else { $(this).closest(".amount-wrapper").removeClass("show-bubble"); }
    });

    $(document).on("focusout.fraction", ".ingredient-row .amount-input", function () { $(this).closest(".amount-wrapper").removeClass("show-bubble"); });

    function showToast(message, isSuccess) {
        var $toast = $("#form-toast-notification");
        $toast.css("background-color", isSuccess ? "#28a745" : "#dc3545");
        $("#toast-icon").text(isSuccess ? "💾" : "⚠️"); $("#toast-message").text(message);
        $toast.css({ "transform": "translateY(0)", "opacity": "1", "pointer-events": "auto" });
        setTimeout(function () { $toast.css({ "transform": "translateY(-20px)", "opacity": "0", "pointer-events": "none" }); }, 3500);
    }

    // --- DYNAMIC UNIT FILTERING ENGINE ---
    function filterMeasurementsByIngredient($ingredientSelect) {
        var $measurementSelect = $ingredientSelect.closest(".ingredient-row").find("select").eq(1);
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
            else if (classification === "DRY") matchesTrack = ((baseType === 1 && [1, 2, 3, 5].includes(unitId)) || baseType === 2 || (baseType === 4 && unitId !== 19));

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
        // Skip tracking fields if it's the ID itself or hidden action flags
        if ($(this).attr('id') === 'recipeId' || $(this).attr('name') === 'actionType') return;

        clearTimeout(autosaveTimeout);
        autosaveTimeout = setTimeout(function() {
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

            localStorage.setItem(STORAGE_KEY, JSON.stringify(savePayload));
        }, 1000); // 1-second delay
    });

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
                var newRowHtml = '<div class="ingredient-row row-flex-alignment-grid">' +
                    '<select class="form-input select-width-weight-large" required>' + ingredientOptions + '</select>' +
                    '<div class="amount-wrapper"><input type="number" step="0.01" class="form-input amount-input input-forced-full-width" placeholder="Amount" required /><div class="fraction-bubble"></div></div>' +
                    '<select class="form-input select-width-weight-medium" required>' + measurementOptions + '</select>' +
                    '<input type="text" class="form-input select-width-weight-large" placeholder="Note (e.g., melted, chilled)" />' +
                    '<button type="button" class="btn remove-ing-btn btn-square-row-dismiss">×</button></div>';
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
            } else {
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
        
        // 🌟 NEW SAFETY CHECK: Find out what recipe ID is in the saved draft vs the page DOM
        var currentFormId = $("#recipeId").val() || "";
        var draftFormId = "";
        
        // Find the recipeId parameter hidden inside the serialized formData payload
        $.each(parsedDraft.formData, function(i, field) {
            if (field.name === "recipeId") {
                draftFormId = field.value || "";
            }
        });

        // 🌟 ONLY proceed if we are making a brand new recipe OR editing the exact same recipe ID
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
});