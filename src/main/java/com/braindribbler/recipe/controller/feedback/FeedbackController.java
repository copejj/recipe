package com.braindribbler.recipe.controller.feedback;

import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.security.RecipeUserDetails;
import com.braindribbler.recipe.service.feedback.FeedbackAndChangelogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;

@Controller
@RequestMapping("/feedback")
public class FeedbackController {

    private final FeedbackAndChangelogService feedbackService;

    public FeedbackController(FeedbackAndChangelogService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping
    public String showFeedbackForm(@RequestParam(required = false) String type,
                                   @RequestParam(required = false) String from,
                                   Model model) {
        model.addAttribute("defaultType", "BUG".equalsIgnoreCase(type) ? "BUG" : "SUGGESTION");
        model.addAttribute("fromUrl", from);
        return "feedback/create";
    }

    @PostMapping("/submit")
    public String submitFeedback(@RequestParam(defaultValue = "SUGGESTION") String feedbackType,
                                 @RequestParam(required = false) String subject,
                                 @RequestParam String description,
                                 @RequestParam(required = false) String pageUrl,
                                 @RequestParam(required = false) String errorRef,
                                 HttpServletRequest request,
                                 @AuthenticationPrincipal RecipeUserDetails currentUser,
                                 RedirectAttributes redirectAttributes) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        User user = currentUser.getUserProfile();
        String userAgent = request.getHeader("User-Agent");

        try {
            feedbackService.submitFeedback(feedbackType, subject, description, pageUrl, errorRef, userAgent, user);
            redirectAttributes.addFlashAttribute("feedbackSuccessMessage",
                    "Thanks! Your report has been sent to the kitchen team.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("feedbackErrorMessage", e.getMessage());
            return "redirect:/feedback";
        }

        if ("ERROR_REPORT".equalsIgnoreCase(feedbackType)) {
            return "redirect:/";
        }

        String safeReturnPath = resolveSafeLocalRedirect(pageUrl);
        return "redirect:" + safeReturnPath;
    }

    private String resolveSafeLocalRedirect(String pageUrl) {
        if (pageUrl == null || pageUrl.isBlank()) return "/";
        try {
            URI parsed = URI.create(pageUrl.trim());
            String path = parsed.getPath();
            if (path == null || !path.startsWith("/") || path.startsWith("//")
                    || path.equals("/error") || path.equals("/feedback")) {
                return "/";
            }
            return parsed.getQuery() != null ? path + "?" + parsed.getQuery() : path;
        } catch (Exception e) {
            return "/";
        }
    }
}
