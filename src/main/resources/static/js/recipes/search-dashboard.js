$(document).ready(function () {
    var tableId = '#generalIndexTable';

    // 1. Pack configuration into a clean, reusable function to prevent layout freezing on updates
    function initializeDataTable() {
        if ($.fn.DataTable.isDataTable(tableId)) {
            $(tableId).DataTable().destroy();
        }

        $(tableId).DataTable({
            "paging": true,
            "searching": true,
            "order": [[3, 'asc']],
            "ordering": true,
            "info": true,
            "pageLength": -1, // Displays ALL records by default on page load
            "lengthMenu": [[10, 25, 50, -1], [10, 25, 50, "All"]],
            "language": {
                "emptyTable": "No data available in table"
            },
            "columnDefs": [
                { "orderable": false, "targets": 0 }, // Disable sorting on actions
                { "width": "5rem", "targets": 0 }
            ],
            "autoWidth": false // Prevents DataTables from guessing and forcing bad layout stretching
        });
    }

    // Initialize the grid layout on first load
    initializeDataTable();

    var selectedIngredients = {};

    // Read any existing filtering tokens pre-loaded into the DOM by Thymeleaf on load
    $('#fridgeInventoryList li').each(function () {
        var id = $(this).attr('data-id');
        var name = $(this).find('span').first().text();
        if (id) {
            selectedIngredients[id] = name;
        }
    });

    // Initialize Select2 search control
    $('.select2-quick-add').select2({
        placeholder: "Type ingredients (e.g. Eggs, Basil)...",
        allowClear: true,
        dropdownAutoWidth: true,
        width: '100%'
    });

    // Intercept dropdown selection events
    $('.select2-quick-add').on('select2:select', function (e) {
        var data = e.params.data;
        var ingredientId = data.id;
        var ingredientName = data.text;

        if (!ingredientId) return;

        // Prevent duplicate items from being added to the list
        if (selectedIngredients[ingredientId]) {
            $('.select2-quick-add').val('').trigger('change');
            return;
        }

        selectedIngredients[ingredientId] = ingredientName;

        var listItemHtml =
            '<li data-id="' + ingredientId + '" class="badge-permission">' +
            '<span>' + ingredientName + '</span>' +
            '<span class="remove-fridge-item">X</span>' +
            '</li>';

        $('#fridgeInventoryList').append(listItemHtml);
        $('.select2-quick-add').val('').trigger('change');
        syncHiddenFormInputs();
        
        // REFRESH CATALOG VIA AJAX FOR A SEAMLESS UX
        refreshRecipeCatalog(); 
    });

    // Handle removal clicks on individual list badges
    $(document).on('click', '.remove-fridge-item', function () {
        var $listItem = $(this).closest('li');
        var targetId = $listItem.attr('data-id');
        delete selectedIngredients[targetId];
        $listItem.remove();
        syncHiddenFormInputs();
        
        // REFRESH CATALOG VIA AJAX UPON INGREDIENT REMOVAL
        refreshRecipeCatalog(); 
    });

    // INSTANTLY RETRIGGER UNIFIED SEARCH UPON CATEGORY SELECTION DEVIATION
    $(document).on('change', '#recipeCategoryFilterSelect', function () {
        refreshRecipeCatalog();
    });

    // Maps active cache identifiers to hidden input tags
    function syncHiddenFormInputs() {
        var $container = $('#hiddenInputsContainer');
        $container.empty();
        Object.keys(selectedIngredients).forEach(function (id) {
            var hiddenInput = $('<input type="hidden" name="ingredientIds" />').val(id);
            $container.append(hiddenInput);
        });
    }

    // ROW CLICK LISTENER FOR DATA TABLE INTERACTION
    $(document).on("click", "#generalIndexTable tbody tr", function (event) {
        if ($(event.target).closest(".btn-view").length) return;
        var href = $(this).data("href");
        if (href) {
            window.location.href = href;
        }
    });

    // Clean Interception for manual/fallback form submissions
    $('#searchForm').on('submit', function(event) {
        event.preventDefault(); // Stop standard full-page reload
        refreshRecipeCatalog();
    });

    // The AJAX function that reloads the recipe index seamlessly
    function refreshRecipeCatalog() {
        var $form = $('#searchForm');
        // Build URL matching the backend parameters
        var targetUrl = $form.attr('action') + '?' + $form.serialize();

        // Dim the table components to give visual loading feedback
        $('.table-responsive-wrapper, .index-section-header').css({
            'opacity': '0.5',
            'transition': 'opacity 0.15s ease'
        });

        // Fetch the updated view fragments silently over AJAX
        $.get(targetUrl, function(responseHtml) {
            // Parse out just the responsive table wrapper from the incoming payload
            var newTable = $(responseHtml).find('.table-responsive-wrapper').html();
            // Parse out the dynamic section title headers
            var newHeader = $(responseHtml).find('.index-section-header').html();
            
            // Inject them back into the DOM safely
            $('.table-responsive-wrapper').html(newTable);
            $('.index-section-header').html(newHeader);

            initializeDataTable();

            // Restore clarity to the updated view
            $('.table-responsive-wrapper, .index-section-header').css('opacity', '1');
        }).fail(function() {
            // Fallback safety if the endpoint encounters an unexpected glitch
            $('.table-responsive-wrapper, .index-section-header').css('opacity', '1');
        });
    }
});
