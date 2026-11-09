$(document).ready(function () {
    var tableId = '#roleMatrixTable';

    // 1. Teardown the generic initial boilerplate engine safely
    if ($.fn.DataTable.isDataTable(tableId)) {
        $(tableId).DataTable().destroy();
    }

    // 2. Build out explicit column distributions and configuration overrides
    $(tableId).DataTable({
        "paging": true,
        "searching": true,
        "ordering": true,
        "order": [[1, 'desc']],
        "info": true,
        "pageLength": 25,
        "lengthMenu": [[10, 25, 50, -1], [10, 25, 50, "All"]],
        "autoWidth": false, // CRITICAL: Stop DataTables from calculating text strings
        "language": {
            "emptyTable": "No data available in table"
        },
        "columnDefs": [
            { "orderable": false, "targets": 0 }, // Disable sorting on Action Buttons
            { "width": "3rem", "targets": 0 }, 
            { "width": "5rem", "targets": 1 }, 
            { "width": "15rem", "targets": 2 } 
        ],
        
    })
});

function openModal(id) { document.getElementById(id).showModal(); }
function closeModal(id) { document.getElementById(id).close(); }