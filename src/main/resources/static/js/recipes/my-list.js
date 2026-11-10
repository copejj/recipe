$(document).ready(function () {
    // 1. Destroy the generic initialization so we can re-map custom layouts
    var tableId = '#myRecipesTable';
    if ($.fn.DataTable.isDataTable(tableId)) {
        $(tableId).DataTable().destroy();
    }

    // 2. Re-initialize with explicit width controls and all-row paging defaults
    $(tableId).DataTable({
        "paging": true,
        "searching": true,
        "order": [[1, 'asc']],
        "ordering": true,
        "info": true,
        "pageLength": -1, // <-- Core Fix: Displays ALL records by default on page load
        "lengthMenu": [[10, 25, 50, -1], [10, 25, 50, "All"]],
        "language": {
            "emptyTable": "No data available in table"
        },
        "columnDefs": [
            { "orderable": false, "targets": 0 }, // Disable sorting on actions
            { "width": "5rem", "targets": 0 }   // Snap Actions Column tight to button edge
        ],
        "autoWidth": true 
    });

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