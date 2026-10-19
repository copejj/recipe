/**
 * Recipes Dynamic Form Processing Engine
 * Handles step cloning, ingredient rows addition/removal, real-time validations, and CSRF modal quick-adds.
 */
$(document).ready(function () {
    var activeSelectDropdown = null;
    var modal = document.getElementById("quickIngredientModal");

    // Change from ".ingredient-row select:first-child" to just "select"
    $(document).on("change select2:select", ".ingredient-row select", function (e) {
        var $changedSelect = $(this);

        // 1. Always run the duplication validation check
        validateStepIngredients();

        // 2. If the ingredient changed, execute the shared filter worker function
        if ($changedSelect.is("select:first-child")) {
            filterMeasurementsByIngredient($changedSelect);
        }
    });

    $("#add-step-btn").click(function () {
        var firstCard = $("#steps-container .step-card").first();

        firstCard.find(".ingredient-row select:first-child").each(function () {
            if ($(this).hasClass("select2-hidden-accessible")) { $(this).select2("destroy"); }
        });

        var clone = firstCard.clone();

        // Reset basic texts and values
        clone.find("textarea, input").val("");
        clone.find("select").prop("selectedIndex", 0);

        // Remove any hidden primary key inputs that were copied over
        clone.find("input[type='hidden']").remove();

        // CHANGED: Completely empty out all ingredient rows for the new step card
        clone.find(".ingredients-container").empty();

        $("#steps-container").append(clone);
        reindexForm();
    });

    $(document).on("click", ".remove-step-btn", function () {
        $(this).closest(".step-card").remove();
        reindexForm();
        validateStepIngredients(); // Revalidate immediately in case a duplicate row was deleted
    });

    $(document).off("click", ".add-ingredient-btn").on("click", ".add-ingredient-btn", function (e) {
        e.preventDefault();

        var container = $(this).closest(".step-card").find(".ingredients-container");
        var ingredientOptions = $('#ingredient-options-pool').html();
        var measurementOptions = $('#measurement-options-pool').html();

        var newRowHtml =
            '<div class="ingredient-row" style="display: flex; gap: 10px; align-items: center; margin-bottom: 10px;">' +
            '<select class="form-input" style="flex: 2.5;" required>' +
            ingredientOptions +
            '</select>' +
            '<div class="amount-wrapper">' +
            '<input type="number" step="0.01" class="form-input amount-input" placeholder="Amount" style="width: 100%;" required />' +
            '<div class="fraction-bubble"></div>' +
            '</div>' +
            '<select class="form-input" style="flex: 1.5;" required>' +
            measurementOptions +
            '</select>' +
            '<input type="text" class="form-input" placeholder="Note (e.g., melted, chilled)" style="flex: 2;" />' +
            '<button type="button" class="btn remove-ing-btn" style="width: auto; padding: 10px 14px; font-size: 1rem; margin: 0; background-color: #e9ecef; color: var(--color-text-dark); border: 1px solid var(--color-border-input); border-radius: 6px;">×</button>' +
            '</div>';

        container.append(newRowHtml);
        reindexForm();
    });

    $(document).on("click", ".remove-ing-btn", function () {
        var $row = $(this).closest(".ingredient-row");
        // Clean tear-down preventing phantom element memory leaks
        $row.find("select:first-child").select2("destroy");
        $row.remove();
        reindexForm();
        validateStepIngredients();
    });

    // --- QUICK INGREDIENT MODAL HANDLERS ---
    $(document).on("click", ".open-quick-ing-btn", function () {
        activeSelectDropdown = $(this).siblings(".ingredients-container").find(".ingredient-row:last-child select").first();
        $("#quickIngredientName").val("");
        $("#quickGroceryCategoryId").val("");
        $("#quickIngredientClassification").val("SOLID"); // Reset dropdown state to SOLID default
        $("#modal-feedback-msg").hide().text("");
        if (modal) modal.showModal();
    });

    $("#closeQuickIngBtn").click(function () {
        if (modal) modal.close();
    });

    // --- SAFE SELECT2 INITIALIZER WRAPPER ---
    function initializeSelect2Search() {
        $(".ingredient-row select:first-child").each(function () {
            // Guard clause: If Select2 is already attached to this element, destroy it first.
            if ($(this).hasClass("select2-hidden-accessible")) {
                $(this).select2("destroy");
            }

            // Apply a fresh, beautifully searchable autocomplete instance
            $(this).select2({
                placeholder: "-- Choose Ingredient --",
                width: "resolve", // Automatically snaps perfectly to your CSS/flexbox grid width
                dropdownAutoWidth: true
            });
        });
    }

    // --- REAL-TIME VALIDATION ENGINE ---
    function validateStepIngredients() {
        $(".ingredient-row").removeClass("row-error-highlight");
        $(".ingredient-error-msg").remove();

        var formIsValid = true;

        $("#steps-container .step-card").each(function () {
            var $stepCard = $(this);
            var seenIngredients = {};

            $stepCard.find(".ingredient-row").each(function () {
                var $row = $(this);
                var selectedIngredientId = $row.find("select").eq(0).val();

                if (!selectedIngredientId) return;

                if (seenIngredients[selectedIngredientId]) {
                    formIsValid = false;
                    seenIngredients[selectedIngredientId].addClass("row-error-highlight");
                    $row.addClass("row-error-highlight");

                    if ($row.next(".ingredient-error-msg").length === 0) {
                        var ingredientName = $row.find("select").eq(0).find("option:selected").text();
                        $row.after('<span class="ingredient-error-msg">⚠️ "' + ingredientName + '" is listed multiple times in this step. Please combine them into a single row.</span>');
                    }
                } else {
                    seenIngredients[selectedIngredientId] = $row;
                }
            });
        });

        return formIsValid;
    }

    // --- REINDEXING LOGIC ---
    function reindexForm() {
        $("#steps-container .step-card").each(function (stepIdx) {
            $(this).attr("data-step-index", stepIdx);
            $(this).find(".step-number-label").text(stepIdx + 1);
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

        initializeSelect2Search();
    }

    // --- HELPER: CONVERT CULINARY DECIMALS TO KITCHEN FRACTIONS ---
    function formatCulinaryFraction(value) {
        var num = parseFloat(value);
        if (isNaN(num) || num <= 0) return "";

        var firstPart = Math.floor(num);
        var decimalPart = num - firstPart;

        decimalPart = Math.round(decimalPart * 1000) / 1000;
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

    // --- CARTOON TALKING BUBBLE EVENTS ---
    $(document).on("input.fraction change.fraction focusin.fraction", ".ingredient-row .amount-input", function () {
        var $input = $(this);
        var $wrapper = $input.closest(".amount-wrapper");
        var $bubble = $wrapper.find(".fraction-bubble");
        var fractionString = formatCulinaryFraction($input.val());

        if (fractionString && $bubble.length) {
            $bubble.text("🍳 Kitchen Fraction: " + fractionString);
            $wrapper.addClass("show-bubble");
        } else {
            $wrapper.removeClass("show-bubble");
        }
    });

    $(document).on("focusout.fraction", ".ingredient-row .amount-input", function () {
        $(this).closest(".amount-wrapper").removeClass("show-bubble");
    });

    // --- APP TOAST NOTIFICATION ENGINE ---
    function showToast(message, isSuccess) {
        var $toast = $("#form-toast-notification");

        if (isSuccess) {
            $toast.css("background-color", "#28a745"); // Green Success
            $("#toast-icon").text("💾");
        } else {
            $toast.css("background-color", "#dc3545"); // Red Danger
            $("#toast-icon").text("⚠️");
        }

        $("#toast-message").text(message);

        $toast.css({ "transform": "translateY(0)", "opacity": "1", "pointer-events": "auto" });

        setTimeout(function () {
            $toast.css({ "transform": "translateY(-20px)", "opacity": "0", "pointer-events": "none" });
        }, 3500);
    }

    // --- MID-TIMELINE ELEMENT INSERTION SYSTEM ---
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

        var activeStepCardRow = $(this).closest(".step-card");
        clone.insertAfter(activeStepCardRow);

        reindexForm();
    });

    // --- DYNAMIC UNIT FILTERING ENGINE & HIERARCHY SORTING ---
    function filterMeasurementsByIngredient($ingredientSelect) {
        var $row = $ingredientSelect.closest(".ingredient-row");
        var $measurementSelect = $row.find("select").eq(1);

        var selectedOption = $ingredientSelect.find("option:selected");
        var classification = selectedOption.attr("data-classification");

        var preferMetric = false;

        if (!classification) {
            var allUnits = $('#measurement-options-pool').html();
            $measurementSelect.html(allUnits);
            return;
        }

        var currentUnitValue = $measurementSelect.val();
        var $filteredUnitsContainer = $('<div>');
        var validOptionsArray = [];

        var $masterOptions = $('<div>').append($('#measurement-options-pool').html()).find('option');

        $masterOptions.each(function () {
            var $opt = $(this);
            var unitIdAttr = $opt.attr('data-unit-id');
            if (!unitIdAttr) return;

            var baseType = parseInt($opt.attr('data-base-type'));
            var unitId = parseInt(unitIdAttr);
            var isMetric = $opt.attr('data-is-metric') === 'true';

            var matchesClassificationTrack = false;

            if (classification === "SOLID") {
                matchesClassificationTrack = (baseType === 2 || baseType === 3 || (baseType === 4 && unitId !== 19));
            }
            else if (classification === "WET") {
                matchesClassificationTrack = (baseType === 1 || baseType === 4);
            }
            else if (classification === "DRY") {
                var allowedDryVolumes = [1, 2, 3, 5];
                var isValidVolume = (baseType === 1 && allowedDryVolumes.includes(unitId));
                var isValidWeight = (baseType === 2);
                var isValidEstimate = (baseType === 4 && unitId !== 19);

                matchesClassificationTrack = (isValidVolume || isValidWeight || isValidEstimate);
            }

            if (matchesClassificationTrack) {
                if (baseType === 1 || baseType === 2) {
                    if (isMetric !== preferMetric) {
                        matchesClassificationTrack = false;
                    }
                }
            }

            if (matchesClassificationTrack) {
                validOptionsArray.push($opt);
            }
        });

        validOptionsArray.sort(function (a, b) {
            var rankA = parseInt($(a).attr('data-scaling-rank')) || 0;
            var rankB = parseInt($(b).attr('data-scaling-rank')) || 0;
            return rankA - rankB;
        });

        $filteredUnitsContainer.append('<option value="">-- Unit --</option>');

        $.each(validOptionsArray, function (idx, $sortedOpt) {
            $filteredUnitsContainer.append($sortedOpt);
        });

        $measurementSelect.html($filteredUnitsContainer.html());

        if ($measurementSelect.find('option[value="' + currentUnitValue + '"]').length > 0) {
            $measurementSelect.val(currentUnitValue);
        } else {
            $measurementSelect.val("");
        }
    }

    function filterExistingSavedRowsOnLoad() {
        $(".ingredient-row select:first-child").each(function () {
            var $ingredientSelect = $(this);
            var selectedValue = $ingredientSelect.val();

            if (selectedValue && selectedValue !== "") {
                filterMeasurementsByIngredient($ingredientSelect);
            }
        });
    }

    // --- QUICK INGREDIENT MODAL HANDLERS ---
    $(document).on("click", ".open-quick-ing-btn", function () {
        activeSelectDropdown = $(this).siblings(".ingredients-container").find(".ingredient-row:last-child select").first();
        $("#quickIngredientName").val("");
        $("#quickGroceryCategoryId").val("");
        $("#quickIngredientClassification").val("SOLID");
        $("#modal-feedback-msg").hide().text("");
        if (modal) modal.showModal();
    });

    $("#closeQuickIngBtn").click(function () {
        if (modal) modal.close();
    });

    $("#quickIngredientForm").submit(function (e) {
        e.preventDefault();
        e.stopPropagation();
        $("#modal-feedback-msg").hide();

        var ingredientName = $("#quickIngredientName").val();
        var categoryId = $("#quickGroceryCategoryId").val();
        var classificationType = $("#quickIngredientClassification").val();

        if (!categoryId || categoryId === "") {
            categoryId = null;
        }

        var tokenCookie = document.cookie.split('; ')
            .find(row => row.startsWith('XSRF-TOKEN='));
        var token = tokenCookie ? tokenCookie.split('=')[1] : null;
        var header = "X-XSRF-TOKEN";

        $.ajax({
            url: '/recipes/ingredients/quick-add',
            type: 'POST',
            data: {
                name: ingredientName,
                groceryCategoryId: categoryId,
                classification: classificationType
            },
            beforeSend: function (xhr) {
                if (token) {
                    xhr.setRequestHeader(header, decodeURIComponent(token));
                }
            },
            success: function (response) {
                if (response.success) {
                    // 1. Append the brand-new choice into our master hidden options template pool
                    $('#ingredient-options-pool').append(
                        $('<option>', {
                            value: response.ingredientId,
                            text: response.ingredientName
                        }).attr('data-classification', classificationType)
                    );

                    // --- RE-SORT MASTER POOL ---
                    var $pool = $('#ingredient-options-pool');
                    var $poolPlaceholder = $pool.find('option[value=""]');
                    var nativePoolOptions = $pool.find('option').not('[value=""]').get();

                    nativePoolOptions.sort(function (a, b) {
                        return $(a).text().localeCompare($(b).text());
                    });
                    $pool.empty().append($poolPlaceholder).append(nativePoolOptions);

                    // --- FIXED: CASCADE NEW OPTIONS POOL TO ALL ACTIVE DROPDOWNS ON PAGE ---
                    // This forces every single existing recipe row to instantly recognize the new item
                    $(".ingredient-row select:first-child").each(function () {
                        var $currentSelect = $(this);
                        var savedCurrentValue = $currentSelect.val(); // Cache what the user already picked here

                        // Overwrite native HTML options with the newly expanded pool list
                        $currentSelect.html($('#ingredient-options-pool').html());

                        // Restore selection state safely
                        $currentSelect.val(savedCurrentValue);
                    });

                    // 2. Auto-select the newly quick-added option on the row that requested it
                    if (activeSelectDropdown) {
                        activeSelectDropdown.val(response.ingredientId);

                        // CRITICAL SELECT2 FORCE OVERRIDE: Tells Select2 the DOM changed so it forces a visual repaint!
                        activeSelectDropdown.trigger('change');

                        // Execute your shared measurement logic worker function to load units immediately
                        filterMeasurementsByIngredient(activeSelectDropdown);
                    }

                    if (modal) modal.close();
                } else {
                    $("#modal-feedback-msg").addClass("error-text").text(response.message || "Failed to add ingredient.").show();
                }
            },
            error: function (xhr) {
                console.error("Quick Add Error Details:", xhr.status, xhr.responseText);
                $("#modal-feedback-msg").addClass("error-text").text("An error occurred (" + xhr.status + "). Please check fields and try again.").show();
            }
        });
    });

    // --- UNIFIED BULK RECIPE VALIDATION ENGINE ---
    // Evaluates constraints dynamically: Save Progress allows partial entries, Finish locks down everything.
    function validateRecipeForm(skipMetadata) {
        $(".form-input-error").removeClass("form-input-error");
        $(".select2-selection--single").removeClass("form-input-error");
        $(".ingredient-error-msg, .field-error-msg").remove();

        var isFormValid = true;

        // 1. BASE METADATA FIELDS (Only enforced on Final Submit)
        if (!skipMetadata) {
            // Validate Recipe Title
            var $title = $("#title");
            if ($title.length && $title.val().trim() === "") {
                isFormValid = false;
                $title.addClass("form-input-error");
                $title.after('<span class="field-error-msg" style="color: var(--color-primary); font-size: 0.875rem; font-weight: 600; display: block; margin-top: 4px;">⚠️ Recipe Title is required to publish.</span>');
            }

            // Validate Servings Count
            var $servings = $("#servings");
            if ($servings.length && ($servings.val().trim() === "" || parseInt($servings.val()) <= 0 || isNaN(parseInt($servings.val())))) {
                isFormValid = false;
                $servings.addClass("form-input-error");
                $servings.after('<span class="field-error-msg" style="color: var(--color-primary); font-size: 0.875rem; font-weight: 600; display: block; margin-top: 4px;">⚠️ Servings count must be greater than 0 to publish.</span>');
            }

            // Validate Privacy & Visibility
            var $visibility = $("#recipeVisibilityId");
            if ($visibility.length && ($visibility.val() === "" || $visibility.val() === null)) {
                isFormValid = false;
                $visibility.addClass("form-input-error");
                $visibility.after('<span class="field-error-msg" style="color: var(--color-primary); font-size: 0.875rem; font-weight: 600; display: block; margin-top: 4px;">⚠️ Visibility setting is required to publish.</span>');
            }
        }

        // 2. DYNAMIC STEP CARDS 
        $("#steps-container .step-card").each(function (idx) {
            var $stepCard = $(this);
            var stepNum = idx + 1;
            var $textarea = $stepCard.find("textarea");

            // Only block Save Progress if the user has left absolutely NO instructional text at all
            if (!skipMetadata && $textarea.length && $textarea.val().trim() === "") {
                isFormValid = false;
                $textarea.addClass("form-input-error");
                $textarea.after('<span class="field-error-msg" style="color: var(--color-primary); font-size: 0.875rem; font-weight: 600; display: block; margin-top: 4px; margin-bottom: 10px;">⚠️ Step ' + stepNum + ' is missing instructional text.</span>');
            }

            // 3. INGREDIENT ROWS INSIDE THIS STEP
            $stepCard.find(".ingredients-container .ingredient-row").each(function () {
                var $row = $(this);
                var $ingSel = $row.find("select").eq(0);
                var $amtInp = $row.find(".amount-input");
                var $unitSel = $row.find("select").eq(1);
                var rowHasEmptyField = false;

                // On Final Submit, check if fields are completely unentered
                if (!skipMetadata) {
                    if (!$ingSel.val() || $ingSel.val() === "") {
                        $row.find(".select2-container .select2-selection--single").addClass("form-input-error");
                        rowHasEmptyField = true;
                    }
                    if (!$amtInp.val() || parseFloat($amtInp.val()) <= 0 || isNaN(parseFloat($amtInp.val()))) {
                        $amtInp.addClass("form-input-error");
                        rowHasEmptyField = true;
                    }
                    if (!$unitSel.val() || $unitSel.val() === "") {
                        $unitSel.addClass("form-input-error");
                        rowHasEmptyField = true;
                    }

                    if (rowHasEmptyField) {
                        isFormValid = false;
                        if ($row.next(".field-error-msg").length === 0) {
                            $row.after('<span class="field-error-msg" style="color: var(--color-primary); font-size: 0.875rem; font-weight: 600; display: block; margin-top: 4px; margin-bottom: 10px;">⚠️ This ingredient entry is incomplete. A valid Selection, Amount, and Unit are required.</span>');
                        }
                    }
                }
            });

            // 4. DUPLICATE CHECKER (Always run this on BOTH buttons to protect database constraints)
            var seenIngredients = {};
            $stepCard.find(".ingredients-container .ingredient-row").each(function () {
                var $row = $(this);
                var selectedIngredientId = $row.find("select").eq(0).val();

                if (!selectedIngredientId) return;

                if (seenIngredients[selectedIngredientId]) {
                    isFormValid = false;
                    seenIngredients[selectedIngredientId].find(".select2-container .select2-selection--single").addClass("form-input-error");
                    $row.find(".select2-container .select2-selection--single").addClass("form-input-error");

                    if ($row.next(".ingredient-error-msg").length === 0) {
                        var ingredientName = $row.find("select").eq(0).find("option:selected").text();
                        $row.after('<span class="ingredient-error-msg" style="color: var(--color-primary); font-size: 0.875rem; font-weight: 600; display: block; margin-top: 4px; margin-bottom: 10px;">⚠️ "' + ingredientName + '" is listed multiple times in this step. Please combine them into a single row.</span>');
                    }
                } else {
                    seenIngredients[selectedIngredientId] = $row;
                }
            });
        });

        return isFormValid;
    }

    // --- COMBINED AJAX EXECUTION LAYER ---
    // Enforces strict metadata checks on BOTH buttons simultaneously
    $(document).on("click", "#save-stay-btn, #finish-view-btn", function (e) {
        e.preventDefault();

        var clickedButtonId = $(this).attr("id");
        var saveButton = $(this);
        var originalText = saveButton.html();

        // CHANGED: Force metadata checks (Title, Servings, Visibility) to ALWAYS run on both actions
        var skipMetadataChecks = false;
        var isFormValid = validateRecipeForm(skipMetadataChecks);

        if (!isFormValid) {
            showToast("Validation failed. Please review missing fields.", false);
            if ($(".form-input-error").length) {
                $('html, body').animate({
                    scrollTop: $(".form-input-error").first().offset().top - 100
                }, 400);
            }
            return;
        }

        saveButton.prop("disabled", true).html("Processing Request...");

        var $form = $("#recipeForm").length ? $("#recipeForm") : $(this).closest("form");
        var token = document.cookie.split('; ').find(row => row.startsWith('XSRF-TOKEN='))?.split('=');
        var header = "X-XSRF-TOKEN";

        var submissionPayload = $form.serialize();
        if (clickedButtonId === "finish-view-btn") {
            submissionPayload += "&actionType=FINISH";
        } else {
            submissionPayload += "&actionType=SAVE";
        }

        $.ajax({
            url: $form.attr("action") || window.location.pathname,
            type: "POST",
            data: submissionPayload,
            beforeSend: function (xhr) {
                if (token && token[1]) {
                    xhr.setRequestHeader(header, decodeURIComponent(token[1]));
                }
            },
            success: function (response) {
                saveButton.prop("disabled", false).html(originalText);

                if (response.success) {

                    // 1. DYNAMIC ID INJECTION
                    if (response.recipeId) {
                        var $idInput = $("#recipeId").length ? $("#recipeId") : $form.find("input[name='recipeId']");
                        if ($idInput.length) {

                            var isBrandNew = !$idInput.val();
                            $idInput.val(response.recipeId);

                            if (isBrandNew) {
                                var editUrl = "/recipes/edit/" + response.recipeId;
                                history.replaceState({ recipeId: response.recipeId }, "", editUrl);
                            }
                        }
                    }

                    // 2. ROUTING COMMANDS
                    if (clickedButtonId === "finish-view-btn" && response.redirectUrl) {
                        window.location.href = response.redirectUrl;
                    } else {
                        showToast("Recipe progress saved successfully!", true);
                    }
                } else {
                    showToast(response.message || "Failed to save recipe progress.", false);
                }
            },
            error: function (xhr) {
                saveButton.prop("disabled", false).html(originalText);
                showToast("Server error saving recipe data. Please try again.", false);
            }
        });
    });

    // --- HOVER STYLING TRANSITIONS ---
    $(document).on("mouseenter", ".inline-delete-btn", function () {
        var $btn = $(this);
        if ($btn.attr("data-confirmed") === "false") {
            $btn.css({ "background-color": "#dc3545", "color": "#ffffff" });
        }
    }).on("mouseleave", ".inline-delete-btn", function () {
        var $btn = $(this);
        if ($btn.attr("data-confirmed") === "false") {
            $btn.css({ "background-color": "#ffffff", "color": "#dc3545" });
        }
    });

    // --- STREAMLINED DELETION BLOCK ---
    $(document).on("submit", ".delete-recipe-form", function (e) {
        var $form = $(this);
        var formId = $form.attr("id") || "deleteForm";
        // Find the button associated with this form
        var $btn = $('button[form="' + formId + '"]').length ? $('button[form="' + formId + '"]') : $(".inline-delete-btn");

        // FIRST CLICK: Change button to solid warning state
        if ($btn.length && $btn.attr("data-confirmed") === "false") {
            e.preventDefault(); // Stop the form from submitting right away

            $btn.attr("data-confirmed", "true");
            $btn.html("Are you sure? This cannot be recovered!");
            $btn.css({
                "background-color": "#bd2130", // Active solid warning red
                "color": "#ffffff",
                "border-color": "#bd2130",
                "box-shadow": "0 0 8px rgba(220, 53, 69, 0.4)"
            });

            // Auto-reset fallback: If they don't click it again within 6 seconds, return to normal
            setTimeout(function () {
                if ($btn.attr("data-confirmed") === "true") {
                    resetDeleteButtonState($btn);
                }
            }, 5000);

            return false;
        }

        // SECOND CLICK: If they click it while data-confirmed is "true", allow it to submit directly
        return true;
    });

    // Helper to revert the button back to the hollow style if they change their mind
    function resetDeleteButtonState($btn) {
        if ($btn.length) {
            $btn.attr("data-confirmed", "false");
            $btn.html("🗑️ Delete Recipe");
            $btn.css({
                "background-color": "#ffffff",
                "color": "#dc3545",
                "border-color": "#dc3545",
                "box-shadow": "none"
            });
        }
    }

    reindexForm();
    filterExistingSavedRowsOnLoad();
});
