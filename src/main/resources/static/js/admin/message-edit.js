document.addEventListener("DOMContentLoaded", () => {
    const offsetInput = document.getElementById("tzOffsetMinutes");
    if (offsetInput) {
        offsetInput.value = String(new Date().getTimezoneOffset());
    }

    if (window.jQuery && $.fn.select2) {
        $("#targetUserId").select2({
            width: "100%",
            sorter: function (data) {
                return data.slice().sort(function (a, b) {
                    const aIsGlobal = !a.id || a.id === "";
                    const bIsGlobal = !b.id || b.id === "";

                    if (aIsGlobal && !bIsGlobal) return -1;
                    if (!aIsGlobal && bIsGlobal) return 1;

                    return a.text.localeCompare(b.text, undefined, { sensitivity: "base" });
                });
            }
        });
    }
});
