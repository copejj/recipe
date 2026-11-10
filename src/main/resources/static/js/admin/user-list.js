$(document).ready(function () {
    // 1. Destroy the generic initialization so we can re-map custom layouts
    var tableId = '#userTable';
    if ($.fn.DataTable.isDataTable(tableId)) {
        $(tableId).DataTable().destroy();
    }

    // 2. Re-initialize with explicit width controls and all-row paging defaults
    $(tableId).DataTable({
        "order": [[3, 'desc']],
        "ordering": true,
        "paging": true,
        "searching": true,
        "info": true,
        "pageLength": -1, 
        "lengthMenu": [[10, 25, 50, -1], [10, 25, 50, "All"]],
        "language": {
            "emptyTable": "No data available in table"
        },
        "columnDefs": [
            { "orderable": false, "targets": 0 }, // Disable sorting on actions
            { 
                "targets": 3,                     // Target index 3 is Column 4
                "type": "date",                   // Forces DataTables to treat it as a chronological date
            }
        ],
        "autoWidth": false // Prevents DataTables from guessing and forcing bad layout stretching
    });
});