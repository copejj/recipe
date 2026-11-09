$(document).ready(function () {
    // 1. Destroy the generic initialization so we can re-map custom layouts
    var tableId = '#masterCategoriesTable';
    if ($.fn.DataTable.isDataTable(tableId)) {
        $(tableId).DataTable().destroy();
    }

    // 2. Re-initialize with explicit width controls and all-row paging defaults
    var table = $(tableId).DataTable({
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
            { "width": "5rem", "targets": 0 },   // Snap Actions Column tight to button edge
            { "width": "8rem", "targets": 1 },    // Snap Display Order input tight
            { "type": "num", "targets": 1 }  
        ],
        "autoWidth": false // Prevents DataTables from guessing and forcing bad layout stretching
    });

    // 3. Intercept form submission to inject valid, gapless Spring property paths
    $('form[action*="save-all"]').on('submit', function (e) {
        var index = 0;
        
        // Loop safely through all table rows managed by DataTables
        table.rows().every(function () {
            var rowNode = this.node();
            
            // Locate each input element with the 'data-field' class inside the row
            $(rowNode).find('.data-field').each(function () {
                var fieldName = $(this).data('name'); // Extracts e.g., 'groceryCategoryId'
                
                // Rewrite the name attribute on-the-fly to a format Spring understands perfectly
                $(this).attr('name', 'items[' + index + '].' + fieldName);
            });
            
            index++;
        });
    });
});
