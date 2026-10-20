$(document).ready(function () {

    // --- STREAMLINED DELETION BLOCK ---
    $(document).on("submit", ".delete-user-form", function (e) {
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
            $btn.html("Delete");
            $btn.css({
                "background-color": "#ffffff",
                "color": "#dc3545",
                "border-color": "#dc3545",
                "box-shadow": "none"
            });
        }
    }
});