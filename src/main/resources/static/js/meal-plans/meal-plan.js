document.addEventListener("DOMContentLoaded", () => {
    // 1. Sort recipe options alphabetically inside each <optgroup> before initializing Select2
    const recipeSelect = document.getElementById("recipeId");
    if (recipeSelect) {
        recipeSelect.querySelectorAll("optgroup").forEach((group) => {
            const options = Array.from(group.querySelectorAll("option"));
            options.sort((a, b) => a.textContent.trim().localeCompare(b.textContent.trim(), undefined, { sensitivity: "base" }));
            options.forEach((opt) => group.appendChild(opt));
        });
    }

    if (window.jQuery && $.fn.select2) {
        $(".select2-enable").select2({ width: "100%" });
    }

    // 2. Persist last-used Day/Date, Meal, and Servings while adding multiple recipes to a plan
    const planApp = document.getElementById("mealPlanApp");
    const planId = planApp ? planApp.dataset.planPublicId : "default";
    const addPrefsKey = `meal_plan_add_prefs_${planId}`;

    const daySelect = document.getElementById("dayOfWeek");
    const dateInput = document.getElementById("scheduledDate");
    const mealSelect = document.getElementById("mealType");
    const servingsInput = document.getElementById("servingsOverride");
    const addRecipeForm = recipeSelect ? recipeSelect.closest("form") : null;

    function restoreAddFormPrefs() {
        try {
            const raw = sessionStorage.getItem(addPrefsKey);
            if (!raw) return;
            const prefs = JSON.parse(raw);

            if (daySelect && prefs.dayOfWeek) {
                daySelect.value = prefs.dayOfWeek;
            }
            if (dateInput && prefs.scheduledDate) {
                dateInput.value = prefs.scheduledDate;
            }
            if (mealSelect && prefs.mealType) {
                mealSelect.value = prefs.mealType;
            }
            if (servingsInput && prefs.servingsOverride) {
                servingsInput.value = prefs.servingsOverride;
            }
        } catch (e) { /* Ignore storage errors */ }
    }

    function saveAddFormPrefs() {
        try {
            const prefs = {
                dayOfWeek: daySelect ? daySelect.value : null,
                scheduledDate: dateInput ? dateInput.value : null,
                mealType: mealSelect ? mealSelect.value : null,
                servingsOverride: servingsInput ? servingsInput.value : null
            };
            sessionStorage.setItem(addPrefsKey, JSON.stringify(prefs));
        } catch (e) { /* Ignore storage errors */ }
    }

    restoreAddFormPrefs();

    if (addRecipeForm) {
        addRecipeForm.addEventListener("submit", saveAddFormPrefs);
    }

    // 3. Perspective Tab Switching
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

    // 4. Inline Meal Row Edit / Cancel Handlers
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
