document.addEventListener("DOMContentLoaded", () => {
    const app = document.getElementById("shoppingListApp");
    if (!app) return;

    const listPublicId = app.dataset.listPublicId;
    const storageKey = `shopping_list_cache_${listPublicId}`;
    const statusBadge = document.getElementById("syncStatusBadge");

    function getCookieValue(name) {
        const match = document.cookie.match(new RegExp("(^|;\\s*)" + name + "=([^;]*)"));
        return match ? decodeURIComponent(match[2]) : null;
    }

    function applyRowVisuals(checkbox) {
        const row = checkbox.closest(".recipe-ingredient-row");
        const textSpan = row ? row.querySelector(".shopping-item-text") : null;
        if (!row || !textSpan) return;

        if (checkbox.checked) {
            textSpan.classList.add("crossed-out-text");
            row.classList.add("pantry-owned-item");
        } else {
            textSpan.classList.remove("crossed-out-text");
            row.classList.remove("pantry-owned-item");
        }
    }

    function saveLocalSnapshot() {
        const checkedMap = {};
        document.querySelectorAll(".shopping-item-checkbox").forEach((cb) => {
            checkedMap[cb.dataset.itemId] = cb.checked;
        });

        try {
            localStorage.setItem(storageKey, JSON.stringify(checkedMap));
            // Hook into main.js auto-resume so opening the app at the store returns straight to this list
            localStorage.setItem("active_cooking_recipe", JSON.stringify({
                url: window.location.pathname,
                title: document.title,
                updatedAt: Date.now()
            }));
        } catch (e) { /* Ignore storage quota errors */ }
    }

    async function syncToggleToServer(itemId, isChecked) {
        const xsrfToken = getCookieValue("XSRF-TOKEN");
        const headers = {};
        if (xsrfToken) {
            headers["X-XSRF-TOKEN"] = xsrfToken;
        }

        if (statusBadge) statusBadge.textContent = "Saving...";

        try {
            const response = await fetch(`/shopping-lists/${listPublicId}/items/${itemId}/toggle`, {
                method: "POST",
                headers: headers,
                credentials: "same-origin"
            });
            if (statusBadge) {
                statusBadge.textContent = response.ok ? "Synced" : "Saved locally (offline)";
            }
        } catch (err) {
            if (statusBadge) statusBadge.textContent = "Saved locally (offline)";
        }
    }

    document.querySelectorAll(".shopping-item-checkbox").forEach((cb) => {
        applyRowVisuals(cb);
        cb.addEventListener("change", () => {
            applyRowVisuals(cb);
            saveLocalSnapshot();
            syncToggleToServer(cb.dataset.itemId, cb.checked);
        });
    });

    window.addEventListener("online", () => {
        if (statusBadge) statusBadge.textContent = "Online • Synced";
    });

    saveLocalSnapshot();
});
