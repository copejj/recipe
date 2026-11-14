$(document).ready(function () {
    var tableId = '#generalCookbookTable';

    // Global client-side tracking state variables
    let activeChefFilter = null;
    let activeCategoryFilter = null;

    function initializeDataTable() {
        if ($.fn.DataTable.isDataTable(tableId)) {
            $(tableId).DataTable().destroy();
        }

        var table = $(tableId).DataTable({
            "paging": true,
            "searching": true,
            "order": [[2, 'asc']], // Orders by Recipe Title (Column Index 2 after removing Actions)
            "ordering": true,
            "info": true,
            "pageLength": -1,
            "lengthMenu": [[10, 25, 50, -1], [10, 25, 50, "All"]],
            "language": {
                "emptyTable": "No data available in table"
            },
            "autoWidth": false
        });

        // Sync visual highlighted classes on initial renders or subsequent paginations
        highlightMatchingBadges();
        return table;
    }

    // Initialize application catalog context state
    var dataTableInstance = initializeDataTable();

    // Custom DataTables engine layout search intercept push hook
    $.fn.dataTable.ext.search.push(function (settings, data, dataIndex) {
        if (settings?.nTable?.id && settings.nTable.id !== 'generalCookbookTable') return true;

        // Column Indices: 0 = Category, 1 = Chef, 2 = Title
        const rowCategoryName = data && data[0] ? data[0].trim() : '';
        const rowChefName = data && data[1] ? data[1].trim() : '';

        if (activeCategoryFilter && rowCategoryName !== activeCategoryFilter) return false;
        if (activeChefFilter && rowChefName !== activeChefFilter) return false;

        return true;
    });

    function highlightMatchingBadges() {
        if (dataTableInstance) {
            // Use the rows().nodes() API to access ALL rows in memory (visible + hidden)
            dataTableInstance.rows().nodes().to$().each(function () {
                $(this).find(".clickable-chef-badge").each(function () {
                    const name = $(this).text().trim();
                    $(this).toggleClass("filter-matching", !!(activeChefFilter && name === activeChefFilter));
                });

                $(this).find(".clickable-category-badge").each(function () {
                    const category = $(this).text().trim();
                    $(this).toggleClass("filter-matching", !!(activeCategoryFilter && category === activeCategoryFilter));
                });
            });
        } else {
            document.querySelectorAll(".clickable-chef-badge").forEach(badge => {
                const name = badge.textContent.trim();
                badge.classList.toggle("filter-matching", !!(activeChefFilter && name === activeChefFilter));
            });

            document.querySelectorAll(".clickable-category-badge").forEach(badge => {
                const category = badge.textContent.trim();
                badge.classList.toggle("filter-matching", !!(activeCategoryFilter && category === activeCategoryFilter));
            });
        }
    }

    // Unified Interceptor handling redirects and badge selections
    $(document).on("click", "#generalCookbookTable tbody tr", function (event) {
        const $target = $(event.target);

        // Let native links (such as the Recipe Title link) or buttons handle their own clicks
        if ($target.closest("a, button").length) return;

        // Chef badge click toggle mechanics
        if ($target.hasClass("clickable-chef-badge")) {
            const clickedChef = $target.text().trim();
            activeChefFilter = (activeChefFilter === clickedChef) ? null : clickedChef;
            highlightMatchingBadges();
            dataTableInstance.draw();
            return;
        }

        // Category badge click toggle mechanics
        if ($target.hasClass("clickable-category-badge")) {
            const clickedCategory = $target.text().trim();
            activeCategoryFilter = (activeCategoryFilter === clickedCategory) ? null : clickedCategory;
            highlightMatchingBadges();
            dataTableInstance.draw();
            return;
        }

        // Ignore row click if the user is highlighting text to copy
        if (window.getSelection && window.getSelection().toString().length > 0) return;

        // Standard Row Navigation Redirection
        var href = $(this).data("href");
        if (href) {
            window.location.href = href;
        }
    });

    /* =========================================================================
       FRIDGE INVENTORY CONTROL PIPELINES
       ========================================================================= */
    var selectedIngredients = {};

    $('#fridgeInventoryList li').each(function () {
        var id = $(this).attr('data-id');
        var name = $(this).find('span').first().text();
        if (id) selectedIngredients[id] = name;
    });

    if ($('.select2-quick-add').length) {
        $('.select2-quick-add').select2({
            placeholder: "Type ingredients (e.g. Eggs, Basil)...",
            allowClear: true,
            dropdownAutoWidth: true,
            width: '100%'
        });

        $('.select2-quick-add').on('select2:select', function (e) {
            var data = e.params.data;
            if (!data.id || selectedIngredients[data.id]) {
                $('.select2-quick-add').val('').trigger('change');
                return;
            }

            selectedIngredients[data.id] = data.text;

            var $li = $('<li></li>')
                .attr('data-id', data.id)
                .addClass('badge-permission')
                .append($('<span></span>').text(data.text))
                .append($('<span></span>').addClass('remove-fridge-item').text('X'));

            $('#fridgeInventoryList').append($li);
            $('.select2-quick-add').val('').trigger('change');
            syncHiddenFormInputs();
            refreshRecipeCatalog();
        });
    }

    $(document).on('click', '.remove-fridge-item', function () {
        var $listItem = $(this).closest('li');
        delete selectedIngredients[$listItem.attr('data-id')];
        $listItem.remove();
        syncHiddenFormInputs();
        refreshRecipeCatalog();
    });

    function syncHiddenFormInputs() {
        var $container = $('#hiddenInputsContainer').empty();
        Object.keys(selectedIngredients).forEach(function (id) {
            $container.append($('<input type="hidden" name="ingredientIds" />').val(id));
        });
    }

    function refreshRecipeCatalog() {
        var $form = $('#searchForm');
        var targetUrl = $form.attr('action') + '?' + $form.serialize();

        $('.table-responsive-wrapper, .index-section-header').css({
            'opacity': '0.5',
            'transition': 'opacity 0.15s ease'
        });

        $.get(targetUrl, function (responseHtml) {
            var $parsed = $('<div></div>').html(responseHtml);
            $('.table-responsive-wrapper').html($parsed.find('.table-responsive-wrapper').html());
            $('.index-section-header').html($parsed.find('.index-section-header').html());

            // Re-instantiate table API context shell; custom engine filters survive redraw updates automatically
            dataTableInstance = initializeDataTable();

            $('.table-responsive-wrapper, .index-section-header').css('opacity', '1');
        }).fail(function () {
            $('.table-responsive-wrapper, .index-section-header').css('opacity', '1');
        });
    }
});
