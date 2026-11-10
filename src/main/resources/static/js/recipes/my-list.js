$(document).ready(function () {
    var tableId = '#myRecipesTable';
    if ($.fn.DataTable.isDataTable(tableId)) {
        $(tableId).DataTable().destroy();
    }

    var table = $(tableId).DataTable({
        "paging": true,
        "searching": true,
        "order": [[2, 'asc']],
        "ordering": true,
        "info": true,
        "pageLength": -1, 
        "lengthMenu": [[10, 25, 50, -1], [10, 25, 50, "All"]],
        "language": {
            "emptyTable": "No data available in table"
        },
        "columnDefs": [
            { "orderable": false, "targets": 0 }, 
            { "width": "5rem", "targets": 0 }   
        ],
        "autoWidth": true,
        "stripeClasses": ['', 'row-even'] 
    });

    // Track the active chef filter state
    let activeChefFilter = null;

    // Register DataTables Custom Row Filter Engine
    $.fn.dataTable.ext.search.push(function (settings, data, dataIndex) {
        if (!activeChefFilter) {
            return true;
        }
        // Column index 1 is the "Chef" name text column
        const rowChefName = data[1].trim(); 
        return rowChefName === activeChefFilter;
    });

    // Helper function to color all matching badges across pages
    function highlightMatchingChefs(selectedChef) {
        // Find every chef badge currently loaded in the entire table DOM tree
        document.querySelectorAll(".table-chef-badge").forEach(badge => {
            const currentBadgeName = badge.textContent.trim();
            
            if (selectedChef && currentBadgeName === selectedChef) {
                badge.classList.add("filter-matching");
            } else {
                badge.classList.remove("filter-matching");
            }
        });
    }

    // Handle updates when DataTables updates/re-draws pages or column orders
    table.on('draw', function () {
        highlightMatchingChefs(activeChefFilter);
    });

    // Central Event Listener Interceptor
    const myRecipesTable = document.getElementById("myRecipesTable");
    if (myRecipesTable) {
        myRecipesTable.addEventListener("click", function (event) {
            const chefBadge = event.target.closest(".table-chef-badge");
            
            // Handle Chef Badge Toggle Interaction
            if (chefBadge) {
                const clickedChef = chefBadge.textContent.trim();

                if (activeChefFilter === clickedChef) {
                    // UNFILTER
                    activeChefFilter = null;
                } else {
                    // FILTER
                    activeChefFilter = clickedChef;
                }

                // Apply highlighting classes to matching names across the table viewport
                highlightMatchingChefs(activeChefFilter);
                
                // Force DataTables to re-evaluate rows and draw
                table.draw();
                return; 
            }

            // Standard Row Tapping Navigation Block
            if (event.target.closest(".btn-action-bypass") || event.target.closest(".admin-btn-group")) {
                return;
            }

            const row = event.target.closest("tr");
            if (row && row.dataset.href) {
                window.location.href = row.dataset.href;
            }
        });
    }
});
