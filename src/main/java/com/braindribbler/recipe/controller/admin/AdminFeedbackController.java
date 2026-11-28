package com.braindribbler.recipe.controller.admin;

import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.security.RecipeUserDetails;
import com.braindribbler.recipe.service.feedback.FeedbackAndChangelogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin")
public class AdminFeedbackController {

    private final FeedbackAndChangelogService feedbackService;

    public AdminFeedbackController(FeedbackAndChangelogService feedbackService) {
        this.feedbackService = feedbackService;
    }

    // --- FEEDBACK LIST & EDIT ---
    @GetMapping("/feedback")
    @PreAuthorize("hasAuthority('can_view_errors')")
    public String listFeedback(Model model) {
        model.addAttribute("feedbackList", feedbackService.getAllFeedback());
        model.addAttribute("openCount", feedbackService.getOpenFeedbackCount());
        return "admin/feedback/list";
    }

    @GetMapping("/feedback/edit/{id}")
    @PreAuthorize("hasAuthority('can_view_errors')")
    public String editFeedback(@PathVariable Integer id, Model model) {
        model.addAttribute("feedback", feedbackService.getFeedbackById(id));
        return "admin/feedback/edit";
    }

    @PostMapping("/feedback/edit/{id}")
    @PreAuthorize("hasAuthority('can_view_errors')")
    public String updateFeedback(@PathVariable Integer id,
                                 @RequestParam String status,
                                 @RequestParam(required = false) String adminNotes,
                                 RedirectAttributes redirectAttributes) {
        feedbackService.updateFeedbackTriage(id, status, adminNotes);
        redirectAttributes.addFlashAttribute("successMessage", "Feedback item #" + id + " updated.");
        return "redirect:/admin/feedback";
    }

    @PostMapping("/feedback/delete/{id}")
    @PreAuthorize("hasAuthority('can_view_errors')")
    public String deleteFeedback(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        feedbackService.deleteFeedback(id);
        redirectAttributes.addFlashAttribute("successMessage", "Feedback item #" + id + " deleted.");
        return "redirect:/admin/feedback";
    }

    // --- ERROR LOGS LIST & EDIT ---
    @GetMapping("/errors")
    @PreAuthorize("hasAuthority('can_view_errors')")
    public String listErrorLogs(Model model) {
        model.addAttribute("errorLogs", feedbackService.getRecentErrorLogs());
        return "admin/errors/list";
    }

    @GetMapping("/errors/edit/{id}")
    @PreAuthorize("hasAuthority('can_view_errors')")
    public String editErrorLog(@PathVariable Integer id, Model model) {
        model.addAttribute("errorLog", feedbackService.getErrorLogById(id));
        return "admin/errors/edit";
    }

    @PostMapping("/errors/delete/{id}")
    @PreAuthorize("hasAuthority('can_view_errors')")
    public String deleteErrorLog(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        feedbackService.deleteErrorLog(id);
        redirectAttributes.addFlashAttribute("successMessage", "Error log #" + id + " deleted.");
        return "redirect:/admin/errors";
    }

    @PostMapping("/errors/purge-unlinked")
    @PreAuthorize("hasAuthority('can_view_errors')")
    public String purgeUnlinkedErrorLogs(RedirectAttributes redirectAttributes) {
        int removed = feedbackService.purgeAllUnreferencedErrorLogs();
        redirectAttributes.addFlashAttribute("successMessage", "Purged " + removed + " unlinked error log(s).");
        return "redirect:/admin/errors";
    }

    // --- WHAT'S NEW ---
    @GetMapping("/whats-new")
    @PreAuthorize("hasAuthority('can_manage_messages')")
    public String manageWhatsNew(Model model) {
        model.addAttribute("entries", feedbackService.getAllChangelogAdmin());
        model.addAttribute("todayDate", LocalDate.now());
        return "admin/feedback/whats-new";
    }

    @PostMapping("/whats-new/save")
    @PreAuthorize("hasAuthority('can_manage_messages')")
    public String saveWhatsNew(@RequestParam(required = false) Integer changelogId,
                               @RequestParam String title,
                               @RequestParam String summary,
                               @RequestParam(defaultValue = "NEW") String category,
                               @RequestParam(required = false) String versionTag,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate publishedDate,
                               @RequestParam(defaultValue = "false") boolean isPublished,
                               @AuthenticationPrincipal RecipeUserDetails currentUser,
                               RedirectAttributes redirectAttributes) {
        User author = (currentUser != null) ? currentUser.getUserProfile() : null;
        try {
            feedbackService.saveChangelogEntry(changelogId, title, summary, category, versionTag, publishedDate, isPublished, author);
            redirectAttributes.addFlashAttribute("successMessage", "Changelog entry saved.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/whats-new";
    }

    @PostMapping("/whats-new/{id}/delete")
    @PreAuthorize("hasAuthority('can_manage_messages')")
    public String deleteWhatsNew(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        feedbackService.deleteChangelogEntry(id);
        redirectAttributes.addFlashAttribute("successMessage", "Changelog entry removed.");
        return "redirect:/admin/whats-new";
    }
}
