
$(document).ready(function () {
    // Eagerly inject CSRF header authorization parameters into all AJAX requests
    $(document).ajaxSend(function (e, xhr, options) {
        var token = $("meta[name='_csrf']").attr("content");
        var header = $("meta[name='_csrf_header']").attr("content");
        if (header && token) {
            xhr.setRequestHeader(header, token);
        }
    });

    function showToast(message, isError = false) {
        var toast = $("#async-toast-alert");
        toast.text(message).css({
            "background-color": isError ? "#fce8e6" : "#e6f4ea",
            "border-color": isError ? "#f4c7c3" : "#bce5c6",
            "color": isError ? "#c5221f" : "#137333"
        }).fadeIn().delay(3000).fadeOut();
    }

    function checkQueueCount() {
        if ($("#queue-tbody tr:visible").length === 0) {
            $("#queue-table-wrapper").hide();
            $("#empty-queue-state").fadeIn();
        }
    }

    $(document).on("click", ".async-approve-btn", function (e) {
        e.preventDefault();
        var button = $(this);
        var ingredientId = button.data("id");

        // Dynamic element ID lookups matching your template output variables precisely
        var updatedNameText = $("#ing-name-input-" + ingredientId).val();
        var updatedCategoryIdVal = $("#ing-category-select-" + ingredientId).val();
        var selectedClassificationTrack = $("#ing-classification-select-" + ingredientId).val(); // ◄ NEW

        // Spring Security CSRF lookup hooks layout configuration rules block
        var token = document.cookie.split('; ').find(row => row.startsWith('XSRF-TOKEN='))?.split('=')[1];

        $.ajax({
            url: '/admin/ingredients/approve/' + ingredientId,
            type: 'POST',
            data: {
                updatedName: updatedNameText,
                updatedCategoryId: updatedCategoryIdVal,
                classification: selectedClassificationTrack // ◄ NEW: Maps cleanly to controller parameters mapping
            },
            beforeSend: function (xhr) {
                if (token) xhr.setRequestHeader("X-XSRF-TOKEN", decodeURIComponent(token));
            },
            success: function (response) {
                if (response.success) {
                    // Smooth row teardown visual sequence animation
                    $("#ing-row-" + ingredientId).fadeOut(300, function () {
                        $(this).remove();
                    });
                } else {
                    alert(response.message || "Approval process failure encountered.");
                }
            },
            error: function () {
                alert("A processing error disrupted dashboard approval validation pipelines.");
            }
        });
    });

    // Asynchronous Merge Action Hook Handler
    $(document).on("click", ".async-merge-btn", function () {
        var duplicateId = $(this).attr("data-id");
        var targetId = $("#merge-target-" + duplicateId).val();
        var targetRow = $("#ing-row-" + duplicateId);

        if (!targetId) {
            showToast("Please choose a valid verification target profile to merge into.", true);
            return;
        }

        $.ajax({
            url: '/admin/ingredients/merge',
            type: 'POST',
            data: {
                duplicateId: duplicateId,
                targetId: targetId
            },
            success: function (response) {
                if (response.success) {
                    targetRow.css("background-color", "#e8f0fe").fadeOut(400, function () {
                        $(this).remove();
                        checkQueueCount();
                    });
                    showToast(response.message);
                }
            },
            error: function (xhr) {
                showToast("Failed to merge duplicate selection.", true);
            }
        });
    });
});