document.addEventListener("DOMContentLoaded", function () {
    // Row tapping navigation listener execution block
    const myRecipesTable = document.getElementById("myRecipesTable");
    if (myRecipesTable) {
        myRecipesTable.addEventListener("click", function (event) {
            // Bypass processing immediately if user taps target button explicitly
            if (event.target.closest(".btn-action-bypass")) {
                return;
            }

            const row = event.target.closest("tr");
            if (row && row.dataset.href) {
                window.location.href = row.dataset.href;
            }
        });
    }
});