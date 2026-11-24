// --- AUTO-RESUME ACTIVE COOKING / SHOPPING RECIPE ---
(function () {
    const path = window.location.pathname;
    // Only auto-resume when landing on your home/root page
    if (path !== "/" && path !== "/index") return;

    // Allow bypassing the redirect if ?home=1 is in the URL or if navigating internally from your own site
    const isManualHomeClick = window.location.search.indexOf("home=1") !== -1;
    const isInternalNavigation = document.referrer && document.referrer.indexOf(window.location.origin) === 0;
    if (isManualHomeClick || isInternalNavigation) return;

    const raw = localStorage.getItem("active_cooking_recipe");
    if (!raw) return;

    try {
        const data = JSON.parse(raw);
        // Keep active for 12 hours (covers building a grocery list earlier in the day)
        const maxAgeMs = 12 * 60 * 60 * 1000;
        if (data.url && (Date.now() - data.updatedAt) < maxAgeMs) {
            window.location.replace(data.url);
        } else {
            localStorage.removeItem("active_cooking_recipe");
        }
    } catch (e) {
        localStorage.removeItem("active_cooking_recipe");
    }
})();

document.addEventListener("DOMContentLoaded", () => {
    // 1. Run once for all standard Thymeleaf elements on page load
    formatDisplayTimestamps();

    // 2. Global listener hook for DataTables lifecycle loops
    // This catches rows generated on page navigation, sorting, and filter checks
    $(document).on('draw.dt', () => {
        formatDisplayTimestamps();
    });

    // 3. Clear active cooking recipe pointer when signing out
    $(document).on('submit', '.nav-logout-form', () => {
        try {
            localStorage.removeItem("active_cooking_recipe");
        } catch (e) { /* Ignore storage errors */ }
    });

    // 4. Calculate stacked offsets for sticky headers (.action-tray + DataTable thead)
    updateStickyStack();
    window.addEventListener("resize", updateStickyStack);
    $(document).on("draw.dt", updateStickyStack);

    // 5. Update smart back/breadcrumb links from same-origin referrer
    initSmartBackLinks();

    // 6. Initialize dismissible dashboard broadcast messages
    initDismissibleDashboardMessages();
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
    // Scan the DOM for all active date tracking vectors
    document.querySelectorAll(".local-time").forEach(timeElement => {
        const rawTimestamp = timeElement.getAttribute("datetime");
        
        if (rawTimestamp) {
            const dateObj = new Date(rawTimestamp);
            
            // Fail-safe protection check for invalid or null timestamp payloads
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
        .filter(el => el.offsetParent !== null); // Skip hidden elements (e.g., mobile card view)

    if (!items.length) return;

    // 1. Find the tallest visible element at each sticky level
    const maxHeightByLevel = {};
    items.forEach((el) => {
        const level = parseInt(el.dataset.stickyLevel || "1", 10);
        const height = el.offsetHeight;
        if (!maxHeightByLevel[level] || height > maxHeightByLevel[level]) {
            maxHeightByLevel[level] = height;
        }
    });

    // 2. Sort active levels ascending (1, 2, 3...) and calculate cumulative top offsets
    const sortedLevels = Object.keys(maxHeightByLevel)
        .map(Number)
        .sort((a, b) => a - b);

    const topOffsetByLevel = {};
    let cumulativeOffset = 0;

    sortedLevels.forEach((level) => {
        topOffsetByLevel[level] = cumulativeOffset;
        cumulativeOffset += maxHeightByLevel[level];
    });

    // 3. Apply calculated top offset and descending z-index by level
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
    // Ignore clicks on native links, buttons, or form inputs so they work normally
    if ($(event.target).closest("a, button, input, label, select").length) return;


    // Ignore click if the user is selecting text to copy
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
        // Only trust same-origin referrers, and ignore self-redirects (e.g., after POST/redirect)
        if (refUrl.origin !== window.location.origin || refUrl.pathname === window.location.pathname) {
            return;
        }

        const labelMap = [
            { prefix: "/shopping-lists/", label: "← Back to Shopping List" },
            { prefix: "/meal-plans/", label: "← Back to Meal Plan" },
            { prefix: "/meal-plans", label: "← Back to Meal Plans" },
            { prefix: "/recipes/my-list", label: "← Back to My Cookbook" },
            { prefix: "/recipes/create", label: "← Back to Recipe Editor" },
            { prefix: "/recipes/edit/", label: "← Back to Recipe Editor" },
            { prefix: "/view/", label: "← Back to Recipe" },
            { prefix: "/search", label: "← Back to Search" }
        ];

        const matched = labelMap.find(entry => refUrl.pathname.startsWith(entry.prefix));

        backLinks.forEach(link => {
            if (refUrl.pathname === "/" || refUrl.pathname === "/index") {
                const homeUrl = new URL(refUrl.href);
                homeUrl.searchParams.set("home", "1");
                link.href = homeUrl.pathname + homeUrl.search;
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
