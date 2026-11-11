$(document).ready(function () {
    // 1. Created Recipes & Saves Breakdown Table
    $('table.stats-table--breakdown').each(function () {
        var $table = $(this);
        if ($.fn.DataTable.isDataTable($table)) {
            $table.DataTable().destroy();
        }

        $table.DataTable({
            "paging": true,
            "searching": true,
            "order": [[2, 'desc']],
            "ordering": true,
            "info": true,
            "language": {
                "emptyTable": "No data available in table"
            },
            "columnDefs": [
                { "orderable": true, "targets": [0, 1, 2] }
            ],
            "autoWidth": true,
            "stripeClasses": ['', 'row-even']
        });
    });

    // 2. Recipes Saved by User Table
    $('table.stats-table--saved').each(function () {
        var $table = $(this);
        if ($.fn.DataTable.isDataTable($table)) {
            $table.DataTable().destroy();
        }

        $table.DataTable({
            "paging": true,
            "searching": true,
            "order": [[0, 'asc']],
            "ordering": true,
            "info": true,
            "language": {
                "emptyTable": "No data available in table"
            },
            "columnDefs": [
                { "orderable": true, "targets": [0, 1] }
            ],
            "autoWidth": true,
            "stripeClasses": ['', 'row-even']
        });
    });
});
