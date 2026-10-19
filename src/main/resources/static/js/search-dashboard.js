$(document).ready(function () {
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
        dropdownAutoWidth: true
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
            '<li data-id="' + ingredientId + '" class="badge-permission" style="padding: 6px 12px; font-size: 0.9rem; border-radius: 20px; display: inline-flex; align-items: center; gap: 10px; background-color: var(--color-badge-blue-bg); color: var(--color-badge-blue-text); border: 1px solid var(--color-border); box-shadow: 0 1px 3px rgba(0,0,0,0.02); transition: all 0.15s ease;">' +
            '<span>' + ingredientName + '</span>' +
            '<span class="remove-fridge-item" style="cursor: pointer; color: var(--color-text-muted); font-weight: 700; transition: color 0.1s;" onmouseover="this.style.color=\'var(--color-danger-raw)\'" onmouseout="this.style.color=\'var(--color-text-muted)\'">X</span>' +
            '</li>';

        $('#fridgeInventoryList').append(listItemHtml);
        $('.select2-quick-add').val('').trigger('change');
        syncHiddenFormInputs();
    });

    // Handle removal clicks on individual list badges
    $(document).on('click', '.remove-fridge-item', function () {
        var $listItem = $(this).closest('li');
        var targetId = $listItem.attr('data-id');
        delete selectedIngredients[targetId];
        $listItem.remove();
        syncHiddenFormInputs();
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

    // Initialize clean DataTables core layout
    $('#generalIndexTable').DataTable({
        paging: true,
        pageLength: 10,
        searching: true,
        ordering: true,
        responsive: true,
        language: {
            searchPlaceholder: "Filter table index..."
        }
    });
});
