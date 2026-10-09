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
     * Renders the unified history view.
     * 🔑 FIXED: Uses your optimized JPQL fetch query returning clean strongly-typed
     * objects.
     */
    @GetMapping
    public String showLoginAuditLogs(Model model) {
        // Natively grabs structured entities instead of raw Object arrays
        List<LoginHistory> logs = loginHistoryRepository.findAllLogsWithUserAuth();

        // Bind cleanly to the model variable your new layout expects
        model.addAttribute("rawLogs", logs);
        return "admin/login-history";
    }

    /**
     * Seamlessly whitelists a connection source for 2 hours.
     */
    @PostMapping("/override")
    public String createIpOverride(@RequestParam("ipAddress") String ipAddress) {

        // Find existing override or instantiate a clean entity slot
        IpOverride override = ipOverrideRepository.findByIpAddress(ipAddress)
                .orElse(new IpOverride());

        override.setIpAddress(ipAddress);
        // Grant a clean, rolling 2-hour exception pass
        override.setOverrideUntil(OffsetDateTime.now().plusHours(2));

        ipOverrideRepository.save(override);

        return "redirect:/admin/login-history?override_success";
    }
}
