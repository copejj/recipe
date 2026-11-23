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
});
