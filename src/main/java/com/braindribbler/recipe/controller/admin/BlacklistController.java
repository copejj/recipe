package com.braindribbler.recipe.controller.admin;

import com.braindribbler.recipe.domain.auth.BlacklistItem;
import com.braindribbler.recipe.service.auth.BlacklistService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/blacklist")
@PreAuthorize("hasAuthority('can_manage_users')")
public class BlacklistController {

    private final BlacklistService blacklistService;

    BlacklistController(BlacklistService blacklistService) {
        this.blacklistService = blacklistService;
    }

    // --- VIEW ROUTERS (Serve Compiled Thymeleaf Pages) ---

    @GetMapping
    public String showListView(Model model) {
        // Fetch and append the active database list directly to the rendering model
        model.addAttribute("blacklist", blacklistService.getAllItems());
        return "admin/blacklist/list";
    }

    @GetMapping("/add")
    public String showAddView(Model model) {
        model.addAttribute("itemId", 0);
        model.addAttribute("item", new BlacklistItem()); // Blank entity wrapper for the template binding form
        return "admin/blacklist/edit";
    }

    @GetMapping("/edit/{id}")
    public String showEditView(@PathVariable Long id, Model model) {
        // Locate the target item to pass down for editing, or handle missing entries
        // safely
        BlacklistItem item = blacklistService.getAllItems().stream()
                .filter(i -> i.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid threat entry ID: " + id));

        model.addAttribute("itemId", id);
        model.addAttribute("item", item);
        return "admin/blacklist/edit";
    }

    // --- POST/PUT HANDLING VIA STANDARD SERVER RENDERING FLOWS ---

    @PostMapping("/save")
    public String saveOrUpdateItem(@ModelAttribute("item") BlacklistItem item, @RequestParam("itemId") Long itemId) {
        if (itemId == 0) {
            blacklistService.addPattern(item);
        } else {
            blacklistService.updatePattern(itemId, item);
        }
        return "redirect:/admin/blacklist"; // Bounces cleanly back to the list index
    }

    @PostMapping("/delete/{id}")
    public String removePattern(@PathVariable Long id) {
        blacklistService.removePattern(id);
        return "redirect:/admin/blacklist"; // Redirects smoothly after unbanning an item
    }
}
