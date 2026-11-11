$(document).ready(function () {
    // 1. Created Recipes & Saves Breakdown Table
    $('table[data-stats-table="breakdown"]').each(function () {
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
            "autoWidth": true,
            "stripeClasses": ['', 'row-even']
        });
    });

    // 2. Recipes Saved by User Table
    $('table[data-stats-table="saved"]').each(function () {
        var $table = $(this);
        if ($.fn.DataTable.isDataTable($table)) {
            $table.DataTable().destroy();
        }

        $table.DataTable({
            "paging": true,
            "searching": true,
            "order": [ [0, 'asc'] ],
            "ordering": true,
            "info": true,
            "autoWidth": true,
            "stripeClasses": ['', 'row-even']
        });
    });
});
