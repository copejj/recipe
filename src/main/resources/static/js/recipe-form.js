/**
 * Recipes Dynamic Form Processing Engine
 * Handles step cloning, ingredient rows addition/removal, real-time validations, and CSRF modal quick-adds.
 */
$(document).ready(function () {
    var activeSelectDropdown = null;
    var modal = document.getElementById("quickIngredientModal");

    // Change from ".ingredient-row select:first-child" to just "select"
    $(document).on("change", ".ingredient-row select", function () {
        validateStepIngredients();
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
        $("#modal-feedback-msg").hide().text("");
        if (modal) modal.showModal();
    });

    $("#closeQuickIngBtn").click(function () {
        if (modal) modal.close();
    });

    $("#quickIngredientForm").submit(function (e) {
        e.preventDefault();
        $("#modal-feedback-msg").hide();

        var ingredientName = $("#quickIngredientName").val();
        var categoryId = $("#quickGroceryCategoryId").val();

        var token = document.cookie.split('; ')
            .find(row => row.startsWith('XSRF-TOKEN='))
            ?.split('=')[1];

        var header = "X-XSRF-TOKEN";

        $.ajax({
            url: '/recipes/ingredients/quick-add',
            type: 'POST',
            data: {
                name: ingredientName,
                groceryCategoryId: categoryId
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
                        new Option(response.ingredientName, response.ingredientId, false, false)
                    );

                    // --- RE-SORT MASTER POOL ---
                    var $pool = $('#ingredient-options-pool');
                    var $poolPlaceholder = $pool.find('option[value=""]');

                    // .get() converts the jQuery selection into a clean native JavaScript array
                    var nativePoolOptions = $pool.find('option').not('[value=""]').get();

                    nativePoolOptions.sort(function (a, b) {
                        return $(a).text().localeCompare($(b).text());
                    });
                    $pool.empty().append($poolPlaceholder).append(nativePoolOptions);

                    // 2. Update and re-sort every active dropdown across all step rows
                    $(".ingredient-row select:first-child").each(function () {
                        var $select = $(this);
                        var currentSelectedValue = $select.val(); // Save current selection state

                        $select.append(new Option(response.ingredientName, response.ingredientId, false, false));

                        var $placeholder = $select.find('option[value=""]');

                        // FIXED: Extracted to native element array to clear line 199 crash!
                        var nativeSelectOptions = $select.find('option').not('[value=""]').get();

                        nativeSelectOptions.sort(function (a, b) {
                            return $(a).text().localeCompare($(b).text());
                        });

                        // Append elements back rearranged without breaking existing selections
                        $select.empty().append($placeholder).append(nativeSelectOptions);
                        $select.val(currentSelectedValue);
                    });

                    // 3. Auto-select the newly added option on the active dropdown
                    if (activeSelectDropdown) {
                        activeSelectDropdown.val(response.ingredientId);
                    }

                    if (modal) modal.close();
                    validateStepIngredients();
                } else {
                    $("#modal-feedback-msg").addClass("error-text").text(response.message || "Failed to add ingredient.").show();
                }
            },
            error: function () {
                $("#modal-feedback-msg").addClass("error-text").text("An error occurred. Please try again.").show();
            }
        });
    });

    // --- FORM SUBMIT INTERCEPTION ---
    $("form").submit(function (e) {
        var isFormValid = validateStepIngredients();

        if (!isFormValid) {
            e.preventDefault();
            $('html, body').animate({
                scrollTop: $(".row-error-highlight").first().offset().top - 100
            }, 400);
        }
    });

    // --- NEW: SAFE SELECT2 INITIALIZER WRAPPER ---
    function initializeSelect2Search() {
        $(".ingredient-row select:first-child").each(function () {
            // Guard clause: If Select2 is already attached to this element, destroy it first.
            // This prevents nasty visual duplication bugs when reindexForm() runs!
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
            // Put the text in the bubble and toggle the spring-open CSS animation class
            $bubble.text("🍳 Kitchen Fraction: " + fractionString);
            $wrapper.addClass("show-bubble");
        } else {
            $wrapper.removeClass("show-bubble");
        }
    });

    // Hide the bubble when the user clicks away from the input field
    $(document).on("focusout.fraction", ".ingredient-row .amount-input", function () {
        $(this).closest(".amount-wrapper").removeClass("show-bubble");
    });

    // --- AJAX PERSISTENCE ENGINE: SAVE PROGRESS AND STAY ---
    $(document).on("click", "#save-stay-btn", function (e) {
        e.preventDefault();

        // 1. First, run your real-time recipe validation sequence
        if (!validateStepIngredients()) {
            // If duplicate items exist, highlight them and cancel out
            $('html, body').animate({
                scrollTop: $(".row-error-highlight").first().offset().top - 100
            }, 400);
            return;
        }

        var $form = $("#recipeForm");
        var saveButton = $(this);
        var originalText = saveButton.html();

        // Visual loading feedback state
        saveButton.prop("disabled", true).html("⏳ Saving Blueprint...");

        // Grab XSRF parameters out of cookies for Spring Security protection
        var token = document.cookie.split('; ')
            .find(row => row.startsWith('XSRF-TOKEN='))
            ?.split('=')[1];
        var header = "X-XSRF-TOKEN";

        // 2. Transmit the form array directly to your existing Spring PostMapping endpoint
        $.ajax({
            url: $form.attr("action"),
            type: "POST",
            data: $form.serialize(), // Packages up titles, descriptions, steps, and ingredients cleanly
            beforeSend: function (xhr) {
                if (token) {
                    xhr.setRequestHeader(header, decodeURIComponent(token));
                }
            },
            success: function (response) {
                saveButton.prop("disabled", false).html(originalText);
                showToast("Recipe blueprint progress saved successfully!", true);
            },
            error: function (xhr) {
                saveButton.prop("disabled", false).html(originalText);

                showToast("A persistence error occurred while saving. Verify fields.", false);
            }
        });
    });

    // --- APP TOAST NOTIFICATION ENGINE ---
    function showToast(message, isSuccess) {
        var $toast = $("#form-toast-notification");

        // Configure styles based on response validity
        if (isSuccess) {
            $toast.css("background-color", "#28a745"); // Green Success
            $("#toast-icon").text("💾");
        } else {
            $toast.css("background-color", "#dc3545"); // Red Danger
            $("#toast-icon").text("⚠️");
        }

        $("#toast-message").text(message);

        // Slide down and fade in
        $toast.css({ "transform": "translateY(0)", "opacity": "1", "pointer-events": "auto" });

        // Auto-dismiss after 3.5 seconds
        setTimeout(function () {
            $toast.css({ "transform": "translateY(-20px)", "opacity": "0", "pointer-events": "none" });
        }, 3500);
    }


    reindexForm();
});
