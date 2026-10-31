document.addEventListener("DOMContentLoaded", () => {
    // 1. Run once for all standard Thymeleaf elements on page load
    formatDisplayTimestamps();

    // 2. Global listener hook for DataTables lifecycle loops
    // This catches rows generated on page navigation, sorting, and filter checks
    $(document).on('draw.dt', () => {
        formatDisplayTimestamps();
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
