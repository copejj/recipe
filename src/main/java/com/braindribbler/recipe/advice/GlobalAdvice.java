package com.braindribbler.recipe.advice;

import com.braindribbler.recipe.config.EnvironmentConfig;
import com.braindribbler.recipe.domain.recipe.Measurement;
import com.braindribbler.recipe.service.recipe.RecipeService;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.GitProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalAdvice {

    @Value("${app.config.environment}")
    private EnvironmentConfig currentEnv;

    // Thread-safe centralized logger instance for the recipe platform engine
    private static final Logger logger = LoggerFactory.getLogger(GlobalAdvice.class);

    private final GitProperties gitProperties;
    private final RecipeService recipeService;

    // Unified constructor handling both dependencies gracefully
    GlobalAdvice(GitProperties gitProperties, RecipeService recipeService) {
        this.gitProperties = gitProperties;
        this.recipeService = recipeService;
    }

    @ModelAttribute
    public void addSiteInfo(Model model) {

        if (gitProperties != null) {
            String tag = gitProperties.get("closest.tag.name");
            String branch = gitProperties.getBranch();
            String fullHash = gitProperties.get("commit.id");
            String shortHash = gitProperties.getShortCommitId();
            String describe = gitProperties.get("commit.id.describe");

            String displayVersion = "Unknown";

            if (currentEnv == EnvironmentConfig.DEV) {
                // --- LOCAL DEV SUITE ROUTING ---
                if (branch != null && !branch.isEmpty() && !branch.equals(fullHash)) {
                    displayVersion = branch; // Force your branch name (e.g., version-info)
                } else if (shortHash != null && !shortHash.isEmpty()) {
                    displayVersion = shortHash; // Fallback to short hash if detached
                }
            } else {
                // --- NON DEV SUITE ROUTING ---
                if (tag != null && !tag.isEmpty()) {
                    displayVersion = tag; // Always prioritize the release tag (e.g., v1.1.86)
                } else if (describe != null && !describe.isEmpty()) {
                    displayVersion = describe; // Fall back to the full describe pattern
                } else {
                    displayVersion = shortHash; // Ultimate production safety fallback
                }
            }

            model.addAttribute("gitBranch", displayVersion);
            model.addAttribute("gitHash", shortHash);
            model.addAttribute("showGitInfo", true);
        } else {
            model.addAttribute("showGitInfo", false);
        }
    }

    @ModelAttribute
    public void addGlobalMeasurementPool(Model model, HttpServletRequest request) {
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            List<Measurement> measurementsPool = recipeService.getAllMeasurements();

            // Map entities to clean, flat, transaction-safe JSON parameter fragments
            List<Map<String, Object>> flatMatrix = measurementsPool.stream().map(m -> {
                Map<String, Object> map = new HashMap<>();
                map.put("unitName", m.getUnitName());
                map.put("pluralName", m.getPluralName());

                // Maps your new database floor value into the JavaScript JSON output array
                map.put("minPromotionFloor",
                        m.getMinPromotionFloor() != null ? m.getMinPromotionFloor().doubleValue() : 0.85);

                map.put("baseUnitEquivalent", m.getBaseUnitEquivalent().doubleValue());
                map.put("scalingRank", m.getScalingRank().intValue());
                map.put("baseTypeId", m.getBaseType() != null ? m.getBaseType().getBaseTypeId() : null);

                // Pass the database track flags down to the JavaScript engine
                map.put("isPromotable", m.getIsPromotable());
                map.put("isMetric", m.getIsMetric());
                return map;
            }).toList();

            model.addAttribute("globalMeasurementsPool", flatMatrix);
        }
    }

    /**
     * Catches harmless 404 missing resource exceptions (like favicon.ico or
     * chrome-devtools requests)
     * and handles them cleanly without flooding terminal screens with 500 error
     * stack traces.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public String handleMissingStaticResources(NoResourceFoundException ex, HttpServletRequest request, Model model) {
        String uri = request.getRequestURI();
        boolean isChromeProbe = (uri != null && uri.contains(".well-known"));

        if (!isChromeProbe) {
            logger.warn("Static resource routing missed: {}", ex.getMessage());
        }

        model.addAttribute("errorCode", "404");
        model.addAttribute("errorTitle", "Resource Not Found");
        model.addAttribute("errorMessage",
                "The specific static asset, file, or image path requested could not be located on the server.");
        model.addAttribute("showTechnicalDetails", false);

        return "error"; // Directs to templates/error.html
    }


    /**
     * Globally catches manual rank violations or Spring Security access checks
     * across all controllers.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public String handleGlobalAccessDenied(AccessDeniedException ex, Model model) {
        // Log as warning since it's an unauthorized client attempt, not a code crash
        logger.warn("Security boundary infraction caught by global monitor: {}", ex.getMessage());

        model.addAttribute("errorCode", "403");
        model.addAttribute("errorTitle", "Access Denied");
        model.addAttribute("errorMessage",
                "Security Boundary Violation: You do not possess the necessary rank credentials to alter this user registry profile.");
        model.addAttribute("showTechnicalDetails", false); // Keep 403s simple on-screen

        return "error"; // Resolves to templates/error.html
    }

    /**
     * Globally catches deep server infrastructure breaks (such as failing database
     * lookups,
     * null pointer bugs, or cascade constraint failures).
     */
    @ExceptionHandler(Exception.class)
    public String handleGlobalBackendCrashes(Exception ex, Authentication authentication, Model model) {
        // Capture the full structural trace stack context safely within the
        // infrastructure logs
        logger.error("CRITICAL ENGINE FAULT INTERCEPTED: ", ex);

        model.addAttribute("errorCode", "500");
        model.addAttribute("errorTitle", "Kitchen System Failure");

        boolean isDeveloper = false;
        if (authentication != null) {
            // Assess authority pool dynamically for your developer viewing permission
            // string
            isDeveloper = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("can_view_errors"));
        }

        if (isDeveloper) {
            // Personnel in the developer role group get to see technical debugging strings
            // directly in UI
            model.addAttribute("errorMessage",
                    "Developer Diagnostics [" + ex.getClass().getSimpleName() + "]: " + ex.getMessage());
            model.addAttribute("showTechnicalDetails", true);

            // EXTRACT THE FULL STACK TRACE LOG AS A STRING:
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            ex.printStackTrace(pw);
            String fullTraceText = sw.toString();

            model.addAttribute("stackTrace", fullTraceText); // Sent straight to Thymeleaf
        } else {
            // Public standard accounts receive clean, secure error templates
            model.addAttribute("errorMessage",
                    "Our kitchen server encountered an unexpected breakdown inside the recipe data core. Engineers have been logged into the terminal to investigate.");
            model.addAttribute("showTechnicalDetails", false);
        }

        return "error"; // Resolves to templates/error.html
    }
}
