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
});

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
