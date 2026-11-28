package com.braindribbler.recipe.controller.error;

import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.domain.feedback.ErrorLog;
import com.braindribbler.recipe.security.RecipeUserDetails;
import com.braindribbler.recipe.service.feedback.FeedbackAndChangelogService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    private final FeedbackAndChangelogService feedbackService;

    public CustomErrorController(FeedbackAndChangelogService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Authentication authentication, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Throwable throwable = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        String failedUri = FeedbackAndChangelogService.extractOriginalUri(request);

        int statusCode = 500;
        String errorTitle = "Unexpected Error";
        String errorMessage = "An error occurred while processing your request. Please try again later.";

        if (status != null) {
            statusCode = Integer.parseInt(status.toString());
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

        User user = null;
        if (authentication != null && authentication.getPrincipal() instanceof RecipeUserDetails details) {
            user = details.getUserProfile();
        }

        ErrorLog logged = feedbackService.recordErrorEvent(
                statusCode, failedUri, request.getMethod(), throwable, user, request.getHeader("User-Agent"));

        model.addAttribute("errorCode", String.valueOf(statusCode));
        model.addAttribute("errorTitle", errorTitle);
        model.addAttribute("errorMessage", errorMessage);
        model.addAttribute("failedUri", failedUri);
        model.addAttribute("errorRef", logged != null ? logged.getReferenceCode() : null);

        return "error";
    }
}
