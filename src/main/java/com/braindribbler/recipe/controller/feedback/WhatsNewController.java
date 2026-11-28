package com.braindribbler.recipe.controller.feedback;

import com.braindribbler.recipe.service.feedback.FeedbackAndChangelogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WhatsNewController {

    private final FeedbackAndChangelogService feedbackService;

    public WhatsNewController(FeedbackAndChangelogService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping("/whats-new")
    public String viewWhatsNew(Model model) {
        model.addAttribute("entries", feedbackService.getPublishedChangelog());
        return "feedback/whats-new";
    }
}
