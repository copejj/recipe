/**
 * Recipes Dynamic Form Processing Engine
 * Handles step cloning, ingredient rows addition/removal, real-time validations, and CSRF modal quick-adds.
 */
$(document).ready(function () {
    var activeSelectDropdown = null;
    var modal = document.getElementById("quickIngredientModal");

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

    // --- TRIGGER EVENTS ---
    $(document).on("change", ".ingredient-row select:first-child", function () {
        validateStepIngredients();
    });

    $("#add-step-btn").click(function () {
        var firstCard = $("#steps-container .step-card").first();
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
            '<input type="number" step="0.01" class="form-input" placeholder="Amount" style="flex: 1;" required />' +
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
        $(this).closest(".ingredient-row").remove();
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
                    $("select").each(function () {
                        if ($(this).attr('id') === 'ingredient-options-pool' || $(this).prop('name')?.endsWith('.ingredientId')) {
                            $(this).append(new Option(response.ingredientName, response.ingredientId, false, false));
                        }
                    });

                    if (activeSelectDropdown) {
                        activeSelectDropdown.val(response.ingredientId);
                    }
                    if (modal) modal.close();
                    validateStepIngredients(); // Revalidate immediately with the new assignment
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
});
