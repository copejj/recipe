package com.braindribbler.recipe.controller.admin;

import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.domain.auth.UserMessage;
import com.braindribbler.recipe.repository.auth.UserMessageRepository;
import com.braindribbler.recipe.repository.auth.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/admin/messages")
@PreAuthorize("hasAuthority('can_manage_messages')")
public class UserMessageAdminController {

    private static final Set<String> ALLOWED_ALERT_TYPES = Set.of("info", "success", "warning", "danger");
    private static final DateTimeFormatter LOCAL_INPUT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final UserMessageRepository userMessageRepository;
    private final UserRepository userRepository;

    public UserMessageAdminController(UserMessageRepository userMessageRepository,
                                      UserRepository userRepository) {
        this.userMessageRepository = userMessageRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String showMessagesList(Model model) {
        model.addAttribute("messages", userMessageRepository.findAllWithUserOrderByCreatedAtDesc());
        model.addAttribute("now", OffsetDateTime.now());
        return "admin/messages/list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        UserMessage msg = new UserMessage();
        msg.setAlertType("info");
        // Default expiration: 24 hours from now
        OffsetDateTime defaultExpiry = OffsetDateTime.now().plusDays(1);

        model.addAttribute("itemId", 0);
        model.addAttribute("message", msg);
        model.addAttribute("expiresAtLocal", defaultExpiry.format(LOCAL_INPUT_FORMAT));
        model.addAttribute("users", userRepository.findAll());
        return "admin/messages/edit";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        UserMessage msg = userMessageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid message ID: " + id));

        model.addAttribute("itemId", id);
        model.addAttribute("message", msg);
        model.addAttribute("expiresAtLocal", msg.getExpiresAt().format(LOCAL_INPUT_FORMAT));
        model.addAttribute("users", getSortedUsers());

        return "admin/messages/edit";
    }

    @PostMapping("/save")
    public String saveOrUpdateMessage(@RequestParam(value = "itemId", required = false) Integer itemId,
                                      @RequestParam("messageText") String messageText,
                                      @RequestParam(value = "alertType", defaultValue = "info") String alertType,
                                      @RequestParam(value = "targetUserId", required = false) Integer targetUserId,
                                      @RequestParam("expiresAtLocal") String expiresAtLocal,
                                      @RequestParam(value = "tzOffsetMinutes", required = false) Integer tzOffsetMinutes,
                                      RedirectAttributes redirectAttributes) {
        boolean isNew = (itemId == null || itemId == 0);

        if (messageText == null || messageText.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Message text cannot be empty.");
            return isNew ? "redirect:/admin/messages/add" : "redirect:/admin/messages/edit/" + itemId;
        }

        UserMessage entity = isNew
                ? new UserMessage()
                : userMessageRepository.findById(itemId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid message ID: " + itemId));

        entity.setMessageText(messageText.trim());

        String normalizedType = alertType != null ? alertType.toLowerCase().trim() : "info";
        entity.setAlertType(ALLOWED_ALERT_TYPES.contains(normalizedType) ? normalizedType : "info");

        if (targetUserId != null && targetUserId != 0) {
            User targetUser = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Selected user not found."));
            entity.setUser(targetUser);
        } else {
            entity.setUser(null);
        }

        LocalDateTime parsedLocal = LocalDateTime.parse(expiresAtLocal, LOCAL_INPUT_FORMAT);
        ZoneOffset offset = (tzOffsetMinutes != null)
                ? ZoneOffset.ofTotalSeconds(-tzOffsetMinutes * 60)
                : OffsetDateTime.now().getOffset();
        entity.setExpiresAt(parsedLocal.atOffset(offset));

        userMessageRepository.save(entity);
        redirectAttributes.addFlashAttribute("successMessage",
                isNew ? "Broadcast message published!" : "Message updated successfully.");
        return "redirect:/admin/messages";
    }

    @PostMapping("/expire/{id}")
    public String expireMessageNow(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        userMessageRepository.findById(id).ifPresent(msg -> {
            msg.setExpiresAt(OffsetDateTime.now().minusMinutes(1));
            userMessageRepository.save(msg);
        });
        redirectAttributes.addFlashAttribute("successMessage", "Message expired immediately.");
        return "redirect:/admin/messages";
    }

    @PostMapping("/delete/{id}")
    public String deleteMessage(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        userMessageRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Message deleted.");
        return "redirect:/admin/messages";
    }

    private List<User> getSortedUsers() {
        return userRepository.findAll().stream()
                .sorted((a, b) -> {
                    String nameA = a.getDisplayName() != null ? a.getDisplayName() : "";
                    String nameB = b.getDisplayName() != null ? b.getDisplayName() : "";
                    return nameA.compareToIgnoreCase(nameB);
                })
                .toList();
    }
}
