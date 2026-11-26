document.addEventListener("DOMContentLoaded", () => {
    const app = document.getElementById("shoppingListApp");
    if (!app) return;

    const listPublicId = app.dataset.listPublicId;
    const storageKey = `shopping_list_cache_${listPublicId}`;
    const dirtyKey = `shopping_list_dirty_${listPublicId}`;
    const showAllPrefKey = "shopping_list_show_all_pref";
    const statusBadge = document.getElementById("syncStatusBadge");
    const showAllBtn = document.getElementById("toggleShowAllBtn");
    const questionBanner = document.getElementById("remoteQuestionBanner");
    const questionBannerText = document.getElementById("remoteQuestionText");
    const jumpToQuestionBtn = document.getElementById("jumpToQuestionBtn");
    const dismissQuestionBannerBtn = document.getElementById("dismissQuestionBannerBtn");
    const extraIngSelect = document.getElementById("extraIngredientId");
    const customNameGroup = document.getElementById("customItemNameGroup");
    const customNameInput = document.getElementById("customItemName");
    const extraCategorySelect = document.getElementById("extraCategoryId");

    let isSavingLocalChange = false;
    let showAllItems = localStorage.getItem(showAllPrefKey) === "true";
    let eventSource = null;
    let pendingQuestionItemId = null;
    let confirmResetTimer = null;

    function syncExtraItemModalFields() {
        if (!extraIngSelect) return;
        const selectedOption = extraIngSelect.options[extraIngSelect.selectedIndex];
        const hasMasterIngredient = Boolean(extraIngSelect.value);

        if (customNameGroup) {
            customNameGroup.classList.toggle("hide-cell", hasMasterIngredient);
        }
        if (customNameInput) {
            customNameInput.required = !hasMasterIngredient;
            if (hasMasterIngredient) customNameInput.value = "";
        }
        if (hasMasterIngredient && selectedOption && selectedOption.dataset.categoryId && extraCategorySelect) {
            extraCategorySelect.value = selectedOption.dataset.categoryId;
        }
    }

    if (extraIngSelect) {
        if (window.jQuery && $.fn.select2) {
            $(extraIngSelect).select2({
                width: "100%",
                dropdownParent: $("#addExtraItemModal")
            });
            $(extraIngSelect).on("change", syncExtraItemModalFields);
        } else {
            extraIngSelect.addEventListener("change", syncExtraItemModalFields);
        }
        syncExtraItemModalFields();
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
                applyRowVisuals(cb);
            }
        });

        if (statusBadge) {
            statusBadge.textContent = navigator.onLine ? "Syncing offline changes..." : "Saved locally (offline)";
        }
    }

    function applyHiddenVisuals(row, isHidden) {
        if (!row) return;
        row.dataset.hidden = String(Boolean(isHidden));
        row.classList.toggle("is-hidden-item", Boolean(isHidden));
        setNotFoundBubble(row, false);

        const textSpan = row.querySelector(".shopping-item-text");
        let badge = row.querySelector(".badge-skipped-item");
        if (isHidden) {
            if (!badge && textSpan) {
                badge = document.createElement("span");
                badge.className = "badge-skipped-item";
                badge.textContent = "Skipped";
                textSpan.appendChild(badge);
            }
        } else if (badge) {
            badge.remove();
        }

        syncRowControlsVisibility(row);
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
        syncRowControlsVisibility(row);
    }

    function applyQuestionVisuals(row, isQuestioned, shouldPulse = false) {
        if (!row) return;
        const btn = row.querySelector(".js-toggle-question-btn");
        const bubble = row.querySelector(".chat-bubble--question");

        row.classList.toggle("is-questioned-row", Boolean(isQuestioned));
        if (btn) {
            btn.classList.toggle("is-active", Boolean(isQuestioned));
            btn.setAttribute("aria-pressed", String(Boolean(isQuestioned)));
        }
        if (bubble) {
            bubble.classList.toggle("hide-cell", !isQuestioned);
        }
        if (isQuestioned && shouldPulse) {
            row.classList.remove("is-question-pulse");
            void row.offsetWidth;
            row.classList.add("is-question-pulse");
        }
    }

    function applyQuestionVisuals(row, isQuestioned, shouldPulse = false, showNotFound = false) {
        if (!row) return;
        const btn = row.querySelector(".js-toggle-question-btn");
        const bubble = row.querySelector(".chat-bubble--question");

        row.classList.toggle("is-questioned-row", Boolean(isQuestioned));
        if (btn) {
            btn.classList.toggle("is-active", Boolean(isQuestioned));
            btn.setAttribute("aria-pressed", String(Boolean(isQuestioned)));
        }
        if (bubble) {
            bubble.classList.toggle("hide-cell", !isQuestioned);
        }

        if (isQuestioned) {
            setNotFoundBubble(row, false);
        } else if (showNotFound) {
            setNotFoundBubble(row, true);
        }

        if (isQuestioned && shouldPulse) {
            row.classList.remove("is-question-pulse");
            void row.offsetWidth;
            row.classList.add("is-question-pulse");
        }
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
                const isHidden = row.dataset.hidden === "true";

                if (showAllItems || (!isChecked && !isHidden)) {
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

    function clearAllDeleteConfirmations(exceptBtn = null) {
        document.querySelectorAll(".js-confirm-remove-btn.is-confirming").forEach((btn) => {
            if (btn === exceptBtn) return;
            btn.classList.remove("is-confirming");
            const bubble = btn.closest(".delete-confirm-wrap")?.querySelector(".js-confirm-bubble");
            if (bubble) bubble.classList.add("hide-cell");
        });
    }

    function focusQuestionedRow(itemId) {
        const row = document.querySelector(`.recipe-ingredient-row[data-item-id="${itemId}"]`);
        if (!row) return;

        if (row.classList.contains("hide-cell")) {
            showAllItems = true;
            updateShowAllButtonUI();
            refreshListVisibility();
        }

        row.scrollIntoView({ behavior: "smooth", block: "center" });
        applyQuestionVisuals(row, true, true);
    }

    function syncRowControlsVisibility(row) {
        if (!row) return;
        const cb = row.querySelector(".shopping-item-checkbox");
        const isChecked = cb ? cb.checked : false;
        const isHidden = row.dataset.hidden === "true";

        const questionWrap = row.querySelector(".question-control-wrap");
        const removeForm = row.querySelector(".js-item-remove-form");
        const restoreBtn = row.querySelector(".js-restore-item-btn");

        if (cb) {
            cb.classList.toggle("hide-cell", isHidden);
            cb.disabled = isHidden;
        }
        if (restoreBtn) {
            restoreBtn.classList.toggle("hide-cell", !isHidden);
        }
        if (questionWrap) {
            questionWrap.classList.toggle("hide-cell", isChecked || isHidden);
        }
        if (removeForm) {
            removeForm.classList.toggle("hide-cell", isChecked || isHidden);
        }
    }


    async function syncToggleToServer(itemId, desiredChecked) {
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
                headers: window.getCsrfHeaders(),
                credentials: "same-origin"
            });
            if (response.ok) {
                const data = await response.json();
                setItemDirty(itemId, desiredChecked, false);
                const row = document.querySelector(`.recipe-ingredient-row[data-item-id="${itemId}"]`);
                applyQuestionVisuals(row, Boolean(data.isQuestioned), false);
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

    function setNotFoundBubble(row, show) {
        if (!row) return;
        const notFoundBubble = row.querySelector(".js-not-found-bubble");
        if (notFoundBubble) {
            notFoundBubble.classList.toggle("hide-cell", !show);
        }
    }

    async function syncQuestionToServer(itemId) {
        const row = document.querySelector(`.recipe-ingredient-row[data-item-id="${itemId}"]`);
        const btn = row ? row.querySelector(".js-toggle-question-btn") : null;

        const nextState = btn ? !btn.classList.contains("is-active") : true;
        applyQuestionVisuals(row, nextState, false, !nextState);

        isSavingLocalChange = true;
        if (statusBadge) statusBadge.textContent = "Saving...";

        try {
            const res = await fetch(`/shopping-lists/${listPublicId}/items/${itemId}/question`, {
                method: "POST",
                headers: window.getCsrfHeaders(),
                credentials: "same-origin"
            });
            if (res.ok) {
                const data = await res.json();
                applyQuestionVisuals(row, Boolean(data.isQuestioned), false);
                if (statusBadge) statusBadge.textContent = "Synced";
            }
        } catch (e) {
            if (statusBadge) statusBadge.textContent = "Offline";
        } finally {
            isSavingLocalChange = false;
        }
    }

    async function syncHiddenToServer(itemId) {
        const row = document.querySelector(`.recipe-ingredient-row[data-item-id="${itemId}"]`);
        const nextHidden = row ? row.dataset.hidden !== "true" : true;

        applyHiddenVisuals(row, nextHidden);
        if (nextHidden) applyQuestionVisuals(row, false, false);
        refreshListVisibility();

        isSavingLocalChange = true;
        if (statusBadge) statusBadge.textContent = "Saving...";

        try {
            const res = await fetch(`/shopping-lists/${listPublicId}/items/${itemId}/hide`, {
                method: "POST",
                headers: window.getCsrfHeaders(),
                credentials: "same-origin"
            });
            if (res.ok) {
                const data = await res.json();
                applyHiddenVisuals(row, Boolean(data.isHidden));
                refreshListVisibility();
                if (statusBadge) statusBadge.textContent = "Synced";
            }
        } catch (e) {
            if (statusBadge) statusBadge.textContent = "Offline";
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
                const row = document.querySelector(`.recipe-ingredient-row[data-item-id="${remoteItem.id}"]`);
                const cb = row ? row.querySelector(".shopping-item-checkbox") : null;
                if (!row || !cb) {
                    window.location.reload();
                    return;
                }
                if (!Object.prototype.hasOwnProperty.call(dirty, String(remoteItem.id)) && cb.checked !== remoteItem.checked) {
                    cb.checked = remoteItem.checked;
                    applyRowVisuals(cb);
                    changed = true;
                }
                const localQuestioned = row.classList.contains("is-questioned-row");
                if (localQuestioned !== Boolean(remoteItem.questioned)) {
                    applyQuestionVisuals(row, Boolean(remoteItem.questioned), Boolean(remoteItem.questioned));
                    changed = true;
                }
                const localHidden = row.dataset.hidden === "true";
                if (localHidden !== Boolean(remoteItem.hidden)) {
                    applyHiddenVisuals(row, Boolean(remoteItem.hidden));
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

        let hasConnectedOnce = false;
        eventSource = new EventSource(`/shopping-lists/${listPublicId}/stream`);

        eventSource.onopen = () => {
            if (hasConnectedOnce) syncMissedStateOnce();
            hasConnectedOnce = true;
        };

        eventSource.addEventListener("item-toggled", (event) => {
            if (isSavingLocalChange) return;
            const remoteItem = JSON.parse(event.data);
            const dirty = getDirtyMap();
            if (Object.prototype.hasOwnProperty.call(dirty, String(remoteItem.id))) return;

            const row = document.querySelector(`.recipe-ingredient-row[data-item-id="${remoteItem.id}"]`);
            const cb = row ? row.querySelector(".shopping-item-checkbox") : null;
            if (!row || !cb) {
                window.location.reload();
                return;
            }

            cb.checked = Boolean(remoteItem.checked);
            applyRowVisuals(cb);
            if (typeof remoteItem.questioned === "boolean") {
                applyQuestionVisuals(row, remoteItem.questioned, false);
            }
            saveLocalSnapshot();
            refreshListVisibility();
            if (statusBadge) statusBadge.textContent = "Updated • Synced";
        });

        eventSource.addEventListener("item-questioned", (event) => {
            if (isSavingLocalChange) return;
            const payload = JSON.parse(event.data);
            const row = document.querySelector(`.recipe-ingredient-row[data-item-id="${payload.id}"]`);
            if (!row) return;

            const wasQuestioned = row.classList.contains("is-questioned-row");
            const isQuestioned = Boolean(payload.questioned);
            applyQuestionVisuals(row, isQuestioned, isQuestioned, wasQuestioned && !isQuestioned);

            if (isQuestioned && questionBanner && questionBannerText) {
                pendingQuestionItemId = payload.id;
                questionBannerText.textContent = `❓ ${payload.name}: "Do we have this already?"`;
                questionBanner.classList.remove("hide-cell");
            } else if (!isQuestioned && String(pendingQuestionItemId) === String(payload.id) && questionBanner) {
                questionBanner.classList.add("hide-cell");
            }
            if (statusBadge) statusBadge.textContent = "Updated • Synced";
        });

        eventSource.addEventListener("item-hidden", (event) => {
            if (isSavingLocalChange) return;
            const payload = JSON.parse(event.data);
            const row = document.querySelector(`.recipe-ingredient-row[data-item-id="${payload.id}"]`);
            if (!row) return;

            applyHiddenVisuals(row, Boolean(payload.hidden));
            refreshListVisibility();
            if (statusBadge) statusBadge.textContent = "Updated • Synced";
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

    document.addEventListener("click", (event) => {
        const questionBtn = event.target.closest(".js-toggle-question-btn");
        if (questionBtn) {
            event.preventDefault();
            clearAllDeleteConfirmations();
            syncQuestionToServer(questionBtn.dataset.itemId);
            return;
        }

            const questionTrigger = event.target.closest(".js-toggle-question-btn, .chat-bubble--question");
        if (questionTrigger) {
            event.preventDefault();
            clearAllDeleteConfirmations();
            const wrap = questionTrigger.closest(".question-control-wrap");
            const itemId = questionTrigger.dataset.itemId || wrap?.querySelector(".js-toggle-question-btn")?.dataset.itemId;
            if (itemId) {
                syncQuestionToServer(itemId);
            }
            return;
        }

        const removeTrigger = event.target.closest(".js-confirm-remove-btn, .js-confirm-bubble");
        if (removeTrigger) {
            const wrap = removeTrigger.closest(".delete-confirm-wrap");
            const confirmBtn = wrap ? wrap.querySelector(".js-confirm-remove-btn") : null;
            if (!confirmBtn) return;

            setNotFoundBubble(removeTrigger.closest(".recipe-ingredient-row"), false);

            const isAlreadyConfirming = confirmBtn.classList.contains("is-confirming");
            if (!isAlreadyConfirming) {
                event.preventDefault();
                clearAllDeleteConfirmations(confirmBtn);
                confirmBtn.classList.add("is-confirming");
                const bubble = wrap.querySelector(".js-confirm-bubble");
                if (bubble) bubble.classList.remove("hide-cell");

                clearTimeout(confirmResetTimer);
                confirmResetTimer = setTimeout(() => clearAllDeleteConfirmations(), 3500);
                return;
            }

            // Confirmed (via either clicking "Sure?" bubble or second tap on ✕)
            clearAllDeleteConfirmations();
            const isManual = confirmBtn.dataset.manual === "true";
            if (!isManual) {
                event.preventDefault();
                syncHiddenToServer(confirmBtn.dataset.itemId);
            } else if (removeTrigger.classList.contains("js-confirm-bubble")) {
                // Manually submit the form when clicking the <span> bubble for an Extra item
                event.preventDefault();
                wrap.requestSubmit();
            }
            return;
        }

        const restoreBtn = event.target.closest(".js-restore-item-btn");
        if (restoreBtn) {
            event.preventDefault();
            clearAllDeleteConfirmations();
            syncHiddenToServer(restoreBtn.dataset.itemId);
            return;
        }

        const confirmBtn = event.target.closest(".js-confirm-remove-btn");
        if (confirmBtn) {
            const isAlreadyConfirming = confirmBtn.classList.contains("is-confirming");
            if (!isAlreadyConfirming) {
                event.preventDefault();
                clearAllDeleteConfirmations(confirmBtn);
                confirmBtn.classList.add("is-confirming");
                const bubble = confirmBtn.closest(".delete-confirm-wrap")?.querySelector(".js-confirm-bubble");
                if (bubble) bubble.classList.remove("hide-cell");

                clearTimeout(confirmResetTimer);
                confirmResetTimer = setTimeout(() => clearAllDeleteConfirmations(), 3500);
                return;
            }

            clearAllDeleteConfirmations();
            const isManual = confirmBtn.dataset.manual === "true";
            if (!isManual) {
                event.preventDefault();
                syncHiddenToServer(confirmBtn.dataset.itemId);
            }
            return;
        }

        if (!event.target.closest(".delete-confirm-wrap")) {
            clearAllDeleteConfirmations();
        }
    });

    if (jumpToQuestionBtn) {
        jumpToQuestionBtn.addEventListener("click", () => {
            if (pendingQuestionItemId) {
                focusQuestionedRow(pendingQuestionItemId);
            }
            if (questionBanner) questionBanner.classList.add("hide-cell");
        });
    }

    if (dismissQuestionBannerBtn) {
        dismissQuestionBannerBtn.addEventListener("click", () => {
            if (questionBanner) questionBanner.classList.add("hide-cell");
        });
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
            const row = cb.closest(".recipe-ingredient-row");
            setNotFoundBubble(row, false);
            applyRowVisuals(cb);
            if (cb.checked) {
                applyQuestionVisuals(row, false, false, false);
            }
            refreshListVisibility();
            saveLocalSnapshot();
            syncToggleToServer(cb.dataset.itemId, cb.checked);
        });
    });

    connectLiveStream();

    document.addEventListener("visibilitychange", () => {
        if (!document.hidden) {
            flushDirtyChanges();
            syncMissedStateOnce();
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
        if (eventSource) eventSource.close();
    });

    if (window.RecipeScalingEngine) {
        window.RecipeScalingEngine.updateAll({ scalingFactor: 1, system: "imperial" });
    }
    updateShowAllButtonUI();
    refreshListVisibility();
    saveLocalSnapshot();
    flushDirtyChanges();
});
