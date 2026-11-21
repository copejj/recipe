$(document).on("click", "[data-toggle-password]", function () {
    const fieldId = $(this).attr("data-toggle-password");
    togglePasswordVisibility(fieldId, this);
});

function togglePasswordVisibility(fieldId, button) {
    const passwordInput = document.getElementById(fieldId);
    const icon = button.querySelector('i');
    
    if (passwordInput.type === "password") {
        passwordInput.type = "text";
        icon.classList.remove('fa-eye');
        icon.classList.add('fa-eye-slash');
        button.setAttribute("aria-label", "Hide password");
    } else {
        passwordInput.type = "password";
        icon.classList.remove('fa-eye-slash');
        icon.classList.add('fa-eye');
        button.setAttribute("aria-label", "Show password");
    }
}

function activateProfileTab(targetId) {
    $(".tab-btn").removeClass("active").attr("aria-selected", "false");
    $('.tab-btn[data-target="' + targetId + '"]').addClass("active").attr("aria-selected", "true");
    $(".tab-panel").hide();
    $("#" + targetId).fadeIn(150);
}

$(".tab-btn").on("click", function () {
    var targetId = $(this).data("target");
    activateProfileTab(targetId);

    // Keep URL in sync without reloading
    var tabParam = (targetId === "user-stats-panel") ? "?tab=stats" : "";
    window.history.replaceState(null, "", "/profile" + tabParam);
});

// Open Stats tab automatically if linked via /profile?tab=stats
if (new URLSearchParams(window.location.search).get("tab") === "stats") {
    activateProfileTab("user-stats-panel");
}

