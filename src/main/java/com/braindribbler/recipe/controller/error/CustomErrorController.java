package com.braindribbler.recipe.controller.error;

import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        // Extract the physical HTTP status code (e.g., 404, 500, 403) from the server
        // servlet context
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        String errorTitle = "Unexpected Error";
        String errorMessage = "An error occurred while processing your request. Please try again later.";
        String errorCode = "500";

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            errorCode = String.valueOf(statusCode);

            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                errorTitle = "Page Not Found";
                errorMessage = "The recipe, page, or portal you are looking for doesn't exist or has been moved.";
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                errorTitle = "Access Denied";
                errorMessage = "You do not have the required culinary permissions to access this administrative zone.";
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                errorTitle = "Server Hiccup";
                errorMessage = "Our kitchen server encountered an unexpected error. Our engineers have been alerted.";
            }
        }

        // Bind data parameters cleanly back to your Thymeleaf template layout
        model.addAttribute("errorTitle", errorTitle);
        model.addAttribute("errorMessage", errorMessage);
        model.addAttribute("errorCode", errorCode);

        return "error"; // Points directly to templates/error.html
    }
}
