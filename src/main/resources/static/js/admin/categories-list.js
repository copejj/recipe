$(document).ready(function () {
    // 1. Destroy the generic initialization so we can re-map custom layouts
    var tableId = '#masterCategoriesTable';
    if ($.fn.DataTable.isDataTable(tableId)) {
        $(tableId).DataTable().destroy();
    }

    // 2. Re-initialize with explicit width controls and all-row paging defaults
    $(tableId).DataTable({
        "paging": true,
        "searching": true,
        "ordering": true,
        "info": true,
        "pageLength": -1, // <-- Core Fix: Displays ALL records by default on page load
        "lengthMenu": [[10, 25, 50, -1], [10, 25, 50, "All"]],
        "language": {
            "emptyTable": "No data available in table"
        },
        "columnDefs": [
            { "orderable": false, "targets": 0 }, // Disable sorting on actions
            { "width": "120px", "targets": 0 },   // Snap Actions Column tight to button edge
            { "width": "100px", "targets": 1 }    // Snap Display Order input tight
        ],
        "autoWidth": false // Prevents DataTables from guessing and forcing bad layout stretching
    });
});
