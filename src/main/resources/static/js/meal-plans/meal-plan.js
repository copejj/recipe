document.addEventListener("DOMContentLoaded", () => {
    if (window.jQuery && $.fn.select2) {
        $(".select2-enable").select2({ width: "100%" });
    }

    const tabButtons = document.querySelectorAll(".tab-btn");
    const tabPanels = document.querySelectorAll(".tab-panel");

    tabButtons.forEach((btn) => {
        btn.addEventListener("click", () => {
            tabButtons.forEach((b) => {
                b.classList.remove("active");
                b.setAttribute("aria-selected", "false");
            });

            tabPanels.forEach((panel) => {
                panel.classList.add("hide-cell");
            });

            btn.classList.add("active");
            btn.setAttribute("aria-selected", "true");

            const target = document.getElementById(btn.dataset.target);
            if (target) {
                target.classList.remove("hide-cell");
                if (window.jQuery && $.fn.select2) {
                    $(target).find(".select2-enable").select2({ width: "100%" });
                }
            }
        });
    });

    // --- INLINE MEAL ROW EDIT / CANCEL HANDLERS ---
    function exitRowEditMode(row) {
        if (!row) return;
        row.classList.remove("is-editing-row");
        row.querySelectorAll(".meal-view-val").forEach((el) => el.classList.remove("hide-cell"));
        row.querySelectorAll(".meal-edit-val").forEach((el) => {
            el.classList.add("hide-cell");
            if ("disabled" in el) {
                el.disabled = true;
            }
        });
    }

    function enterRowEditMode(row) {
        if (!row) return;
        // Close any other row currently open for editing so only one row's inputs are enabled
        document.querySelectorAll("#scheduledMealsTable tr.is-editing-row").forEach((otherRow) => {
            if (otherRow !== row) exitRowEditMode(otherRow);
        });

        row.classList.add("is-editing-row");
        row.querySelectorAll(".meal-view-val").forEach((el) => el.classList.add("hide-cell"));
        row.querySelectorAll(".meal-edit-val").forEach((el) => {
            el.classList.remove("hide-cell");
            if ("disabled" in el) {
                el.disabled = false;
            }
        });

        const firstInput = row.querySelector("select.meal-edit-val, input.meal-edit-val");
        if (firstInput) firstInput.focus();
    }

    document.addEventListener("click", (event) => {
        const editBtn = event.target.closest(".js-edit-meal-btn");
        if (editBtn) {
            event.preventDefault();
            enterRowEditMode(editBtn.closest("tr"));
            return;
        }

        const cancelBtn = event.target.closest(".js-cancel-meal-btn");
        if (cancelBtn) {
            event.preventDefault();
            exitRowEditMode(cancelBtn.closest("tr"));
        }
    });
});
