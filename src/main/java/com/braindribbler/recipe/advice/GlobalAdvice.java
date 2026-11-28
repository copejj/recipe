package com.braindribbler.recipe.advice;

import com.braindribbler.recipe.config.EnvironmentConfig;
import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.domain.recipe.Measurement;
import com.braindribbler.recipe.security.RecipeUserDetails;
import com.braindribbler.recipe.service.feedback.FeedbackAndChangelogService;
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

import tools.jackson.databind.ObjectMapper;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalAdvice {

    @Value("${app.config.environment}")
    private EnvironmentConfig currentEnv;

    @Value("${app.api.host}")
    private String apiHost;

    private static final Logger logger = LoggerFactory.getLogger(GlobalAdvice.class);

    private final GitProperties gitProperties;
    private final RecipeService recipeService;
    private final ObjectMapper objectMapper;
    private final FeedbackAndChangelogService feedbackService;

    GlobalAdvice(GitProperties gitProperties,
                 RecipeService recipeService,
                 ObjectMapper objectMapper,
                 FeedbackAndChangelogService feedbackService) {
        this.gitProperties = gitProperties;
        this.recipeService = recipeService;
        this.objectMapper = objectMapper;
        this.feedbackService = feedbackService;
    }

    @ModelAttribute("apiHost")
    public String getApiHost() {
        return apiHost;
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
                if (branch != null && !branch.isEmpty() && !branch.equals(fullHash)) {
                    displayVersion = branch;
                } else if (shortHash != null && !shortHash.isEmpty()) {
                    displayVersion = shortHash;
                }
            } else {
                if (tag != null && !tag.isEmpty()) {
                    displayVersion = tag;
                } else if (describe != null && !describe.isEmpty()) {
                    displayVersion = describe;
                } else {
                    displayVersion = shortHash;
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

            List<Map<String, Object>> flatMatrix = measurementsPool.stream().map(m -> {
                Map<String, Object> map = new HashMap<>();

                map.put("measurementId", m.getMeasurementId());
                map.put("unitName", m.getUnitName());
                map.put("pluralName", m.getPluralName());
                map.put("minPromotionFloor", m.getMinPromotionFloor() != null ? m.getMinPromotionFloor().doubleValue() : 0.85);
                map.put("baseUnitEquivalent", m.getBaseUnitEquivalent().doubleValue());
                map.put("scalingRank", m.getScalingRank().intValue());
                map.put("baseTypeId", m.getBaseType() != null ? m.getBaseType().getBaseTypeId() : null);
                map.put("isPromotable", m.getIsPromotable());
                map.put("isMetric", m.getIsMetric());
                return map;
            }).toList();

            try {
                model.addAttribute("globalMeasurementsPoolJson", objectMapper.writeValueAsString(flatMatrix));
            } catch (Exception e) {
                logger.error("Failed to serialize measurement pool", e);
                model.addAttribute("globalMeasurementsPoolJson", "[]");
            }
        }
    }

    private User resolveUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof RecipeUserDetails details) {
            return details.getUserProfile();
        }
        return null;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public String handleMissingStaticResources(NoResourceFoundException ex,
                                               HttpServletRequest request,
                                               Authentication authentication,
                                               Model model) {
        String uri = request.getRequestURI();
        boolean isChromeProbe = (uri != null && uri.contains(".well-known"));

        if (!isChromeProbe) {
            logger.warn("Static resource routing missed: {}", ex.getMessage());
            var logged = feedbackService.recordErrorEvent(
                    404, FeedbackAndChangelogService.extractOriginalUri(request),
                    request.getMethod(), ex, resolveUser(authentication), request.getHeader("User-Agent"));
            model.addAttribute("errorRef", logged != null ? logged.getReferenceCode() : null);
        }

        model.addAttribute("errorCode", "404");
        model.addAttribute("errorTitle", "Resource Not Found");
        model.addAttribute("errorMessage",
                "The specific static asset, file, or image path requested could not be located on the server.");
        model.addAttribute("failedUri", FeedbackAndChangelogService.extractOriginalUri(request));
        model.addAttribute("showTechnicalDetails", false);

        return "error";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleGlobalAccessDenied(AccessDeniedException ex,
                                           HttpServletRequest request,
                                           Authentication authentication,
                                           Model model) {
        logger.warn("Security boundary infraction caught by global monitor: {}", ex.getMessage());

        var logged = feedbackService.recordErrorEvent(
                403, FeedbackAndChangelogService.extractOriginalUri(request),
                request.getMethod(), ex, resolveUser(authentication), request.getHeader("User-Agent"));

        model.addAttribute("errorCode", "403");
        model.addAttribute("errorTitle", "Access Denied");
        model.addAttribute("errorMessage",
                "Security Boundary Violation: You do not possess the necessary credentials to access this resource.");
        model.addAttribute("failedUri", FeedbackAndChangelogService.extractOriginalUri(request));
        model.addAttribute("errorRef", logged != null ? logged.getReferenceCode() : null);
        model.addAttribute("showTechnicalDetails", false);

        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGlobalBackendCrashes(Exception ex,
                                             HttpServletRequest request,
                                             Authentication authentication,
                                             Model model) {
        logger.error("CRITICAL ENGINE FAULT INTERCEPTED: ", ex);

        var logged = feedbackService.recordErrorEvent(
                500, FeedbackAndChangelogService.extractOriginalUri(request),
                request.getMethod(), ex, resolveUser(authentication), request.getHeader("User-Agent"));

        model.addAttribute("errorCode", "500");
        model.addAttribute("errorTitle", "Kitchen System Failure");
        model.addAttribute("failedUri", FeedbackAndChangelogService.extractOriginalUri(request));
        model.addAttribute("errorRef", logged != null ? logged.getReferenceCode() : null);

        boolean isDeveloper = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("can_view_errors"));

        if (isDeveloper) {
            model.addAttribute("errorMessage",
                    "Developer Diagnostics [" + ex.getClass().getSimpleName() + "]: " + ex.getMessage());
            model.addAttribute("showTechnicalDetails", true);

            java.io.StringWriter sw = new java.io.StringWriter();
            ex.printStackTrace(new java.io.PrintWriter(sw));
            model.addAttribute("stackTrace", sw.toString());
        } else {
            model.addAttribute("errorMessage",
                    "Our kitchen server encountered an unexpected breakdown inside the recipe data core. Engineers have been logged into the terminal to investigate.");
            model.addAttribute("showTechnicalDetails", false);
        }

        return "error";
    }
}
