document.addEventListener("DOMContentLoaded", () => {
    const app = document.getElementById("shoppingListApp");
    if (!app) return;

    const listPublicId = app.dataset.listPublicId;
    const storageKey = `shopping_list_cache_${listPublicId}`;
    const dirtyKey = `shopping_list_dirty_${listPublicId}`;
    const showAllPrefKey = "shopping_list_show_all_pref";
    const statusBadge = document.getElementById("syncStatusBadge");
    const showAllBtn = document.getElementById("toggleShowAllBtn");

    let isSavingLocalChange = false;
    let showAllItems = localStorage.getItem(showAllPrefKey) === "true";
    let eventSource = null;
    let hasConnectedOnce = false;

    eventSource.onopen = () => {
        if (hasConnectedOnce) {
            // Reconnected after 5-min timeout or network blip—catch up any missed toggles
            syncMissedStateOnce();
        }
        hasConnectedOnce = true;
    };

    function getCookieValue(name) {
        const match = document.cookie.match(new RegExp("(^|;\\s*)" + name + "=([^;]*)"));
        return match ? decodeURIComponent(match[2]) : null;
    }

    function getDirtyMap() {
        try {
            return JSON.parse(localStorage.getItem(dirtyKey) || "{}");
        } catch (e) {
            return {};
        }
    }

    function setItemDirty(itemId, checkedState, isDirty) {
        const dirty = getDirtyMap();
        if (isDirty) {
            dirty[String(itemId)] = checkedState;
        } else {
            delete dirty[String(itemId)];
        }
        try {
            localStorage.setItem(dirtyKey, JSON.stringify(dirty));
        } catch (e) { /* Ignore storage errors */ }
    }

    function restoreOfflineChanges() {
        const dirty = getDirtyMap();
        const dirtyIds = Object.keys(dirty);
        if (dirtyIds.length === 0) return;

        dirtyIds.forEach((itemId) => {
            const cb = document.querySelector(`.shopping-item-checkbox[data-item-id="${itemId}"]`);
            if (cb) {
                cb.checked = Boolean(dirty[itemId]);
            }
        });

        if (statusBadge) {
            statusBadge.textContent = navigator.onLine ? "Syncing offline changes..." : "Saved locally (offline)";
        }
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

    function renderOzPackagePreviews() {
        document.querySelectorAll('.shopping-item-text').forEach(row => {
            const amtEl = row.querySelector('.scalable-amount');
            const previewEl = row.querySelector('.oz-package-preview');
            if (!amtEl || !previewEl) return;

            const baseAmount = parseFloat(amtEl.dataset.baseAmount);
            const baseTypeId = parseInt(amtEl.dataset.baseTypeId, 10);
            const baseUnitEq = parseFloat(amtEl.dataset.baseUnitEquivalent);
            const isPromotable = amtEl.dataset.isPromotable === 'true';

            if (!isPromotable || isNaN(baseAmount) || isNaN(baseUnitEq) || baseAmount <= 0) {
                previewEl.textContent = '';
                return;
            }

            // Base type 1 = Volume (tsp -> fl oz: 1 fl oz = 6 tsp), Base type 2 = Weight (g -> oz: 1 oz = 28.3495 g)
            const totalBaseUnits = baseAmount * baseUnitEq;
            if (baseTypeId === 1) {
                const flOz = totalBaseUnits / 6.0;
                previewEl.textContent = ` (${Math.round(flOz * 10) / 10} fl oz)`;
            } else if (baseTypeId === 2) {
                const oz = totalBaseUnits / 28.3495;
                previewEl.textContent = ` (${Math.round(oz * 10) / 10} oz)`;
            }
        });
    }

    function updateShowAllButtonUI() {
        if (!showAllBtn) return;
        showAllBtn.setAttribute("aria-pressed", String(showAllItems));
        showAllBtn.classList.toggle("is-active", showAllItems);
        showAllBtn.textContent = showAllItems ? "Hide Checked" : "Show All";
    }

    function refreshListVisibility() {
        const allDoneBanner = document.getElementById("allItemsDoneMessage");
        let totalVisibleSections = 0;

        document.querySelectorAll(".shopping-category-section").forEach((section) => {
            let visibleItemsInSection = 0;

            section.querySelectorAll(".recipe-ingredient-row").forEach((row) => {
                const cb = row.querySelector(".shopping-item-checkbox");
                const isChecked = cb ? cb.checked : false;

                if (showAllItems || !isChecked) {
                    row.classList.remove("hide-cell");
                    visibleItemsInSection++;
                } else {
                    row.classList.add("hide-cell");
                }
            });

            if (visibleItemsInSection === 0) {
                section.classList.add("hide-cell");
            } else {
                section.classList.remove("hide-cell");
                totalVisibleSections++;
            }
        });

        if (allDoneBanner) {
            if (!showAllItems && totalVisibleSections === 0) {
                allDoneBanner.classList.remove("hide-cell");
            } else {
                allDoneBanner.classList.add("hide-cell");
            }
        }
    }

    function saveLocalSnapshot() {
        const checkedMap = {};
        document.querySelectorAll(".shopping-item-checkbox").forEach((cb) => {
            checkedMap[cb.dataset.itemId] = cb.checked;
        });

        try {
            localStorage.setItem(storageKey, JSON.stringify(checkedMap));
            localStorage.setItem("active_cooking_recipe", JSON.stringify({
                url: window.location.pathname,
                title: document.title,
                updatedAt: Date.now()
            }));
        } catch (e) { /* Ignore storage quota errors */ }
    }
    async function syncToggleToServer(itemId, desiredChecked) {
        const xsrfToken = getCookieValue("XSRF-TOKEN");
        const headers = {};
        if (xsrfToken) {
            headers["X-XSRF-TOKEN"] = xsrfToken;
        }

        if (!navigator.onLine) {
            setItemDirty(itemId, desiredChecked, true);
            if (statusBadge) statusBadge.textContent = "Saved locally (offline)";
            return;
        }

        isSavingLocalChange = true;
        if (statusBadge) statusBadge.textContent = "Saving...";

        try {
            const response = await fetch(`/shopping-lists/${listPublicId}/items/${itemId}/toggle`, {
                method: "POST",
                headers: headers,
                credentials: "same-origin"
            });
            if (response.ok) {
                setItemDirty(itemId, desiredChecked, false);
                if (statusBadge) statusBadge.textContent = "Synced";
            } else {
                setItemDirty(itemId, desiredChecked, true);
                if (statusBadge) statusBadge.textContent = "Saved locally (offline)";
            }
        } catch (err) {
            setItemDirty(itemId, desiredChecked, true);
            if (statusBadge) statusBadge.textContent = "Saved locally (offline)";
        } finally {
            isSavingLocalChange = false;
        }
    }

    async function flushDirtyChanges() {
        if (!navigator.onLine) return;
        const dirty = getDirtyMap();
        const dirtyIds = Object.keys(dirty);
        if (dirtyIds.length === 0) return;

        for (const itemId of dirtyIds) {
            await syncToggleToServer(itemId, dirty[itemId]);
        }
    }

    async function syncMissedStateOnce() {
        if (isSavingLocalChange || !navigator.onLine) return;
        try {
            const res = await fetch(`/shopping-lists/${listPublicId}/state`, { credentials: "same-origin" });
            if (!res.ok) return;
            const data = await res.json();
            const dirty = getDirtyMap();
            const currentCheckboxes = document.querySelectorAll(".shopping-item-checkbox");

            if (data.count !== currentCheckboxes.length) {
                window.location.reload();
                return;
            }

            let changed = false;
            data.items.forEach((remoteItem) => {
                if (Object.prototype.hasOwnProperty.call(dirty, String(remoteItem.id))) return;
                const cb = document.querySelector(`.shopping-item-checkbox[data-item-id="${remoteItem.id}"]`);
                if (!cb) {
                    window.location.reload();
                    return;
                }
                if (cb.checked !== remoteItem.checked) {
                    cb.checked = remoteItem.checked;
                    applyRowVisuals(cb);
                    changed = true;
                }
            });

            if (changed) {
                saveLocalSnapshot();
                refreshListVisibility();
                if (statusBadge) statusBadge.textContent = "Updated • Synced";
            }
        } catch (e) { /* Ignore transient errors */ }
    }

    function connectLiveStream() {
        if (eventSource) {
            eventSource.close();
        }

        eventSource = new EventSource(`/shopping-lists/${listPublicId}/stream`);

        eventSource.addEventListener("item-toggled", (event) => {
            if (isSavingLocalChange) return;

            const remoteItem = JSON.parse(event.data);
            const dirty = getDirtyMap();
            if (Object.prototype.hasOwnProperty.call(dirty, String(remoteItem.id))) {
                return;
            }

            const cb = document.querySelector(`.shopping-item-checkbox[data-item-id="${remoteItem.id}"]`);
            if (!cb) {
                window.location.reload();
                return;
            }

            if (cb.checked !== remoteItem.checked) {
                cb.checked = remoteItem.checked;
                applyRowVisuals(cb);
                saveLocalSnapshot();
                refreshListVisibility();
                if (statusBadge) statusBadge.textContent = "Updated • Synced";
            }
        });

        eventSource.addEventListener("structure-changed", () => {
            window.location.reload();
        });

        eventSource.onerror = () => {
            if (!navigator.onLine && statusBadge) {
                statusBadge.textContent = "Saved locally (offline)";
            }
        };
    }

    if (showAllBtn) {
        showAllBtn.addEventListener("click", () => {
            showAllItems = !showAllItems;
            try {
                localStorage.setItem(showAllPrefKey, String(showAllItems));
            } catch (e) { /* Ignore storage errors */ }
            updateShowAllButtonUI();
            refreshListVisibility();
        });
    }

    restoreOfflineChanges();

    document.querySelectorAll(".shopping-item-checkbox").forEach((cb) => {
        applyRowVisuals(cb);
        cb.addEventListener("change", () => {
            applyRowVisuals(cb);
            refreshListVisibility();
            saveLocalSnapshot();
            syncToggleToServer(cb.dataset.itemId, cb.checked);
        });
    });

    connectLiveStream();

    document.addEventListener("visibilitychange", () => {
        if (!document.hidden) {
            flushDirtyChanges();
            if (!eventSource || eventSource.readyState === EventSource.CLOSED) {
                connectLiveStream();
            }
        }
    });

    window.addEventListener("online", () => {
        if (statusBadge) statusBadge.textContent = "Online • Synced";
        flushDirtyChanges();
        if (!eventSource || eventSource.readyState === EventSource.CLOSED) {
            connectLiveStream();
        }
    });

    window.addEventListener("beforeunload", () => {
        if (eventSource) {
            eventSource.close();
        }
    });

    renderOzPackagePreviews();
    updateShowAllButtonUI();
    refreshListVisibility();
    saveLocalSnapshot();
    flushDirtyChanges();
});
