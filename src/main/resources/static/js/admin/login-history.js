/**
 * Telemetry Monitor Core Orchestrator
 * Integrates precise RegEx boundaries column cross-filtering and window query param interceptors.
 */
window.addEventListener('load', function () {
    var $ = jQuery;

    // Safety guard tear-down preventing double-binding glitches
    if ($.fn.DataTable.isDataTable('#auditTable')) {
        $('#auditTable').DataTable().destroy();
    }

    var table = $('#auditTable').DataTable({
        "order": [[0, 'desc']],
        "pageLength": 25,
        "searching": true,
        "ordering": true,
        "paging": true,
        "columnDefs": [
            {
                "targets": 0,
                "render": function (data, type, row) {
                    if ((type === 'display' || type === 'filter') && data) {
                        var cleanData = data.trim();
                        if (cleanData === '—' || cleanData === '') return '—';

                        var date = new Date(cleanData);
                        if (!isNaN(date.getTime())) {
                            return date.toLocaleString('en-US', {
                                month: 'short',
                                day: 'numeric',
                                year: 'numeric',
                                hour: 'numeric',
                                minute: '2-digit',
                                hour12: true
                            });
                        }
                    }
                    return data;
                }
            }
        ],
        "language": {
            "search": "Filter Logs:",
            "zeroRecords": "No matching authentication records found in logs history"
        }
    });

    // Tracking state coordinates for cross-filtering locks
    var activeUserId = null;
    var activeIpAddress = null;

    // ==========================================================================
    // CORE FILTER APPLICATION WORKER
    // ==========================================================================
    function applyCustomAuditFilters() {
        // Target: Column 1 = User ID; Column 3 = Network IP Address

        if (activeUserId) {
            table.column(1).search('^' + activeUserId + '$', true, false);
            $('#userIdBadgeText').text(activeUserId);
            $('#userIdFilterBadge').fadeIn(150);
        } else {
            table.column(1).search('');
            $('#userIdFilterBadge').fadeOut(150);
        }

        if (activeIpAddress) {
            table.column(3).search('^' + activeIpAddress.replace(/\./g, '\\.') + '$', true, false);
            $('#ipBadgeText').text(activeIpAddress);
            $('#ipFilterBadge').fadeIn(150);
        } else {
            table.column(3).search('');
            $('#ipFilterBadge').fadeOut(150);
        }

        // Redraw table viewport changes
        table.draw();
    }

    // ==========================================================================
    // URL QUERY PARAMETER INTERCEPTOR (ON ROUTE ENGAGEMENT)
    // ==========================================================================
    function checkUrlParametersOnLoad() {
        var urlParams = new URLSearchParams(window.location.search);
        var paramUserId = urlParams.get('userId');
        var paramIpAddress = urlParams.get('ip');

        if (paramUserId) {
            activeUserId = paramUserId.trim();
        }
        if (paramIpAddress) {
            activeIpAddress = paramIpAddress.trim();
        }

        if (activeUserId || activeIpAddress) {
            applyCustomAuditFilters();
        }
    }

    // --- DELEGATED CLICK HANDLER: TOGGLE USER ID FILTER ---
    $(document).on("click", ".user-filter-trigger", function () {
        var clickedUser = $(this).data("user-id").toString();
        activeUserId = (activeUserId === clickedUser) ? null : clickedUser;
        applyCustomAuditFilters();
    });

    // --- DELEGATED CLICK HANDLER: TOGGLE IP ADDRESS FILTER ---
    $(document).on("click", ".ip-filter-trigger", function () {
        var clickedIp = $(this).data("ip-address").toString();
        activeIpAddress = (activeIpAddress === clickedIp) ? null : clickedIp;
        applyCustomAuditFilters();
    });

    // --- CONTROL DECK ALERTS DISMISS ACTIONS ---
    $('#userIdFilterBadge').click(function () {
        activeUserId = null;
        applyCustomAuditFilters();
    });

    $('#ipFilterBadge').click(function () {
        activeIpAddress = null;
        applyCustomAuditFilters();
    });

    // Execute URL parameter evaluation loop instantly on script setup
    checkUrlParametersOnLoad();
});
