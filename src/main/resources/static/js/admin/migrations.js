$(document).ready(function () {
    // 1. Grab the DataTables instance for the migrations table
    var table = $('#migrationsTable').DataTable();

    // 2. Clear out any default order and sort by Column 1 (Filename) DESC
    // 3. Run .draw() to tell DataTables to refresh the UI view
    table.order([[1, 'desc']]).draw();
});
