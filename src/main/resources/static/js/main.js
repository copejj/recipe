document.addEventListener("DOMContentLoaded", () => {
    // 1. Run once for all standard Thymeleaf elements on page load
    formatDisplayTimestamps();

    // 2. Global listener hook for DataTables lifecycle loops
    // This catches rows generated on page navigation, sorting, and filter checks
    $(document).on('draw.dt', () => {
        formatDisplayTimestamps();
    });

    // 3. Calculate stacked offsets for sticky headers (.action-tray + DataTable thead)
    updateStickyStack();
    window.addEventListener("resize", updateStickyStack);
    $(document).on("draw.dt", updateStickyStack);

    // 4. Update smart back/breadcrumb links from same-origin referrer
    initSmartBackLinks();

    // 5. Initialize dismissible dashboard broadcast messages
    initDismissibleDashboardMessages();

    // 6. Initialize feedback modal context & What's New unread indicator
    initFeedbackAndChangelog();
});

function initDismissibleDashboardMessages() {
    const container = document.getElementById("dashboardBroadcastContainer");
    if (!container) return;

    const userId = container.dataset.userId || "anon";
    const storageKey = `dismissed_dashboard_messages_${userId}`;

    let dismissedIds = [];
    try {
        const raw = localStorage.getItem(storageKey);
        if (raw) dismissedIds = JSON.parse(raw);
    } catch (e) {
        dismissedIds = [];
    }

    const dismissedSet = new Set(dismissedIds.map(String));
    const activeIdsOnPage = new Set();

    container.querySelectorAll(".js-dismissible-message").forEach((banner) => {
        const msgId = String(banner.dataset.messageId || "");
        if (!msgId) return;
        activeIdsOnPage.add(msgId);

        if (dismissedSet.has(msgId)) {
            banner.remove();
            return;
        }

        const closeBtn = banner.querySelector(".js-dismiss-message-btn");
        if (closeBtn) {
            closeBtn.addEventListener("click", () => {
                dismissedSet.add(msgId);
                try {
                    localStorage.setItem(storageKey, JSON.stringify(Array.from(dismissedSet)));
                } catch (e) { /* Ignore storage quota errors */ }
                banner.remove();
            });
        }
    });

    // Clean up old expired message IDs from localStorage so it never grows stale
    const prunedIds = Array.from(dismissedSet).filter((id) => activeIdsOnPage.has(id));
    if (prunedIds.length !== dismissedSet.size) {
        try {
            localStorage.setItem(storageKey, JSON.stringify(prunedIds));
        } catch (e) { /* Ignore storage errors */ }
    }
}

function formatDisplayTimestamps() {
    document.querySelectorAll(".local-time").forEach(timeElement => {
        const rawTimestamp = timeElement.getAttribute("datetime");

        if (rawTimestamp) {
            const dateObj = new Date(rawTimestamp);

            if (!isNaN(dateObj.getTime())) {
                const localizedString = dateObj.toLocaleString(navigator.language, {
                    year: 'numeric',
                    month: 'short',
                    day: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit',
                    hour12: true
                });

                timeElement.textContent = localizedString;
            }
        }
    });
}

function updateStickyStack() {
    const items = Array.from(document.querySelectorAll(".sticky-stack-item"))
        .filter(el => el.offsetParent !== null);

    if (!items.length) return;

    const maxHeightByLevel = {};
    items.forEach((el) => {
        const level = parseInt(el.dataset.stickyLevel || "1", 10);
        const height = el.offsetHeight;
        if (!maxHeightByLevel[level] || height > maxHeightByLevel[level]) {
            maxHeightByLevel[level] = height;
        }
    });

    const sortedLevels = Object.keys(maxHeightByLevel)
        .map(Number)
        .sort((a, b) => a - b);

    const topOffsetByLevel = {};
    let cumulativeOffset = 0;

    sortedLevels.forEach((level) => {
        topOffsetByLevel[level] = cumulativeOffset;
        cumulativeOffset += maxHeightByLevel[level];
    });

    items.forEach((el) => {
        const level = parseInt(el.dataset.stickyLevel || "1", 10);
        el.style.top = topOffsetByLevel[level] + "px";
        el.style.zIndex = String(100 - level);
    });
}

document.addEventListener("DOMContentLoaded", () => {
    updateStickyStack();

    const observer = new ResizeObserver(() => {
        updateStickyStack();
    });

    document.querySelectorAll(".sticky-stack-item").forEach((el) => {
        observer.observe(el);
    });
});

$(document).on("click", ".clickable-admin-row", function (event) {
    if ($(event.target).closest("a, button, input, label, select").length) return;
    if (window.getSelection && window.getSelection().toString().length > 0) return;

    const href = $(this).data("href");
    if (href) {
        window.location.href = href;
    }
});

// 1. Delegated confirm() for buttons (replaces th:onclick="return confirm(...)")
$(document).on("click", "[data-confirm]", function (event) {
    const message = $(this).attr("data-confirm");
    if (message && !window.confirm(message)) {
        event.preventDefault();
    }
});

// 2. Delegated confirm() for forms (replaces onsubmit="return confirm(...)")
$(document).on("submit", "form[data-confirm]", function (event) {
    const message = $(this).attr("data-confirm");
    if (message && !window.confirm(message)) {
        event.preventDefault();
    }
});

// 3. Select-all on focus (replaces onfocus="this.select()")
$(document).on("focus", "[data-select-on-focus]", function () {
    this.select();
});

// 4. Auto-initialize generic DataTables (replaces inline <script> in fragments/datatable.html)
document.addEventListener("DOMContentLoaded", () => {
    $("table[data-auto-init='true']").each(function () {
        if (!$.fn.DataTable.isDataTable(this)) {
            $(this).DataTable({
                paging: true,
                searching: true,
                ordering: true,
                info: true,
                pageLength: -1,
                lengthMenu: [[10, 25, 50, -1], [10, 25, 50, "All"]],
                language: {
                    emptyTable: "No data available in table"
                }
            });
        }
    });
});

// 5. Global delegated handlers for <dialog class="admin-modal">
$(document).on("click", "[data-open-modal]", function () {
    const modalId = $(this).data("open-modal");
    const dialog = document.getElementById(modalId);
    if (dialog && typeof dialog.showModal === "function") {
        dialog.showModal();
        const firstInput = dialog.querySelector("input:not([type='hidden']), select, textarea");
        if (firstInput) firstInput.focus();
    }
});

$(document).on("click", "[data-close-modal]", function () {
    const dialog = this.closest("dialog");
    if (dialog) dialog.close();
});

$(document).on("click", "dialog.admin-modal", function (event) {
    if (event.target === this) {
        this.close();
    }
});

function initSmartBackLinks() {
    const backLinks = document.querySelectorAll(".js-smart-back");
    if (!backLinks.length || !document.referrer) return;

    try {
        const refUrl = new URL(document.referrer);
        if (refUrl.origin !== window.location.origin || refUrl.pathname === window.location.pathname) {
            return;
        }

        const labelMap = [
            { prefix: "/shopping-lists/", label: "← Back to Shopping List" },
            { prefix: "/meal-plans/", label: "← Back to Meal Plan" },
            { prefix: "/meal-plans", label: "← Back to Meal Plans" },
            { prefix: "/recipes/cookbook", label: "← Back to My Cookbook" },
            { prefix: "/recipes/create", label: "← Back to Recipe Editor" },
            { prefix: "/recipes/edit/", label: "← Back to Recipe Editor" },
            { prefix: "/view/", label: "← Back to Recipe" },
            { prefix: "/search", label: "← Back to Search" }
        ];

        const matched = labelMap.find(entry => refUrl.pathname.startsWith(entry.prefix));

        backLinks.forEach(link => {
            if (refUrl.pathname === "/" || refUrl.pathname === "/index") {
                link.href = "/";
                link.textContent = "← Home";
            } else if (matched) {
                link.href = refUrl.pathname + refUrl.search;
                link.textContent = matched.label;
            }
        });
    } catch (e) {
        // Keep default fallback href/label if referrer URL is malformed
    }
}

function initFeedbackAndChangelog() {
    // 1. Populate current page URL whenever the global feedback modal opens
    document.addEventListener("click", (event) => {
        const trigger = event.target.closest("[data-open-modal='globalFeedbackModal']");
        if (!trigger) return;

        const pageUrlInput = document.getElementById("feedbackPageUrl");
        if (pageUrlInput) {
            pageUrlInput.value = window.location.pathname + window.location.search;
        }
        const typeSelect = document.getElementById("feedbackTypeSelect");
        if (typeSelect && trigger.dataset.feedbackDefault) {
            typeSelect.value = trigger.dataset.feedbackDefault;
        }
    });

    // 2. Unread "What's New" indicator via localStorage
    const seenKey = "bd_last_seen_changelog_id";
    const feedEl = document.getElementById("whatsNewFeed");
    const footerEl = document.getElementById("globalFooter");
    const latestId = parseInt(
        (feedEl?.dataset.latestChangelogId || footerEl?.dataset.latestChangelogId || "0"),
        10
    );

    if (feedEl && latestId > 0) {
        try {
            localStorage.setItem(seenKey, String(latestId));
        } catch (e) { /* Ignore storage errors */ }
    } else if (latestId > 0) {
        let lastSeen = 0;
        try {
            lastSeen = parseInt(localStorage.getItem(seenKey) || "0", 10);
        } catch (e) { /* Ignore storage errors */ }

        if (latestId > lastSeen) {
            document.querySelectorAll(".js-whats-new-card, a[href$='/whats-new']").forEach((el) => {
                el.classList.add("has-unread-update");
            });
        }
    }

    // 3. Admin "What's New" inline edit button loader
    document.addEventListener("click", (event) => {
        const editBtn = event.target.closest(".js-edit-changelog-btn");
        if (!editBtn) return;

        const form = document.getElementById("changelogEditorForm");
        if (!form) return;

        document.getElementById("editChangelogId").value = editBtn.dataset.id || "";
        document.getElementById("changelogTitle").value = editBtn.dataset.title || "";
        document.getElementById("changelogSummary").value = editBtn.dataset.summary || "";
        document.getElementById("changelogCategory").value = editBtn.dataset.category || "NEW";
        document.getElementById("changelogVersion").value = editBtn.dataset.version || "";
        document.getElementById("changelogDate").value = editBtn.dataset.date || "";
        document.getElementById("changelogPublished").checked = editBtn.dataset.published === "true";

        const heading = document.getElementById("changelogFormHeading");
        if (heading) heading.textContent = "Edit Update #" + editBtn.dataset.id;

        const resetBtn = document.getElementById("resetChangelogFormBtn");
        if (resetBtn) resetBtn.classList.remove("hide-cell");

        form.scrollIntoView({ behavior: "smooth", block: "start" });
    });

    const resetBtn = document.getElementById("resetChangelogFormBtn");
    if (resetBtn) {
        resetBtn.addEventListener("click", () => {
            const form = document.getElementById("changelogEditorForm");
            if (form) form.reset();
            document.getElementById("editChangelogId").value = "";
            const heading = document.getElementById("changelogFormHeading");
            if (heading) heading.textContent = "Post a New Update";
            resetBtn.classList.add("hide-cell");
        });
    }
}

// Global CSRF helper for fetch() POST/PUT/DELETE requests
window.getCsrfToken = function () {
    const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/);
    if (match && match[1]) {
        return decodeURIComponent(match[1]);
    }
    const csrfInput = document.querySelector('input[name="_csrf"]');
    return csrfInput ? csrfInput.value : null;
};

window.getCsrfHeaders = function (extraHeaders = {}) {
    const token = window.getCsrfToken();
    return token ? { ...extraHeaders, "X-XSRF-TOKEN": token } : { ...extraHeaders };
};
