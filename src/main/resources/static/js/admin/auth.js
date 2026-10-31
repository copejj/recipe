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
