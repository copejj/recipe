/* src/main/resources/static/js/datatable-custom.js */

$(document).ready(function () {
    $('.generic-datatable').each(function () {
        var table = $(this);
        var tableId = table.attr('id');

        if (!$.fn.DataTable.isDataTable('#' + tableId)) {
            // Check for HTML data-attribute overrides before defining standard fallbacks
            var autoWidthSetting = table.data('dt-autowidth') !== undefined ? table.data('dt-autowidth') : true;

            table.DataTable({
                "paging": true,
                "searching": true,
                "ordering": true,
                "info": true,
                "pageLength": 10,
                "autoWidth": autoWidthSetting, // Dynamic property assignment
                "language": {
                    "emptyTable": "No data available in table"
                },
                "columnDefs": [
                    { "orderable": false, "targets": 0 }
                ]
            });
        }
    });
});
