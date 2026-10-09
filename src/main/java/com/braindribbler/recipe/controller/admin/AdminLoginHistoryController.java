package com.braindribbler.recipe.controller.admin;

import com.braindribbler.recipe.domain.auth.IpOverride;
import com.braindribbler.recipe.domain.auth.LoginHistory;
import com.braindribbler.recipe.repository.auth.IpOverrideRepository;
import com.braindribbler.recipe.repository.auth.LoginHistoryRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/login-history")
public class AdminLoginHistoryController {

    private final LoginHistoryRepository loginHistoryRepository;
    private final IpOverrideRepository ipOverrideRepository;

    public AdminLoginHistoryController(LoginHistoryRepository loginHistoryRepository,
            IpOverrideRepository ipOverrideRepository) {
        this.loginHistoryRepository = loginHistoryRepository;
        this.ipOverrideRepository = ipOverrideRepository;
    }

    /**
     * Renders the history view with optional user filtering.
     */
    @GetMapping
    public String showLoginAuditLogs(@RequestParam(value = "userId", required = false) Integer userId, Model model) {
        List<Object[]> rawRows;

        if (userId != null) {
            rawRows = loginHistoryRepository.findLogsByUserIdWithActualEmailNative(userId);
            model.addAttribute("selectedUserId", userId);
        } else {
            rawRows = loginHistoryRepository.findAllLogsWithActualEmailNative();
        }

        // Convert raw structural array sets directly into standard model bindings
        // Object mappings index references: [0] = actualEmail, [1..N] are mapped
        // elements
        model.addAttribute("rawLogs", rawRows);
        return "admin/login-history";
    }

    /**
     * Seamlessly whitelists a connection source for 2 hours.
     */
    @PostMapping("/override")
    public String createIpOverride(@RequestParam("ipAddress") String ipAddress,
            @RequestParam(value = "userId", required = false) Integer userId) {

        // Find existing override or instantiate a clean entity slot
        IpOverride override = ipOverrideRepository.findByIpAddress(ipAddress)
                .orElse(new IpOverride());

        override.setIpAddress(ipAddress);
        // Grant a clean, rolling 2-hour exception pass
        override.setOverrideUntil(OffsetDateTime.now().plusHours(2));

        ipOverrideRepository.save(override);

        // Redirect back preserving current user filter context if applicable
        if (userId != null) {
            return "redirect:/admin/login-history?userId=" + userId + "&override_success";
        }
        return "redirect:/admin/login-history?override_success";
    }
}
