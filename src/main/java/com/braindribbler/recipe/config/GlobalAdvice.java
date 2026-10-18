package com.braindribbler.recipe.config;

import com.braindribbler.recipe.domain.recipe.Measurement;
import com.braindribbler.recipe.service.recipe.RecipeService;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.info.GitProperties;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class GlobalAdvice {

    private final GitProperties gitProperties;
    private final RecipeService recipeService; // Injected data service handle

    // Unified constructor handling both dependencies gracefully
    GlobalAdvice(GitProperties gitProperties, RecipeService recipeService) {
        this.gitProperties = gitProperties;
        this.recipeService = recipeService;
    }

    @ModelAttribute
    public void addBuildVersionToModel(Model model) {
        if (gitProperties != null) {
            String tag = gitProperties.get("closest.tag.name");
            String branch = gitProperties.getBranch();
            String fullHash = gitProperties.get("commit.id");
            String shortHash = gitProperties.getShortCommitId();

            String displayVersion;

            if (branch != null && branch.equals(fullHash)) {
                displayVersion = (tag != null && !tag.isEmpty()) ? tag : shortHash;
            } else {
                displayVersion = (branch != null) ? branch : "unknown";
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

                map.put("baseUnitEquivalent", m.getBaseUnitEquivalent().doubleValue());
                map.put("scalingRank", m.getScalingRank().intValue());
                map.put("baseTypeId", m.getBaseType() != null ? m.getBaseType().getBaseTypeId() : null);

                // ADDED: Pass the new database track flags down to the JavaScript engine
                map.put("isPromotable", m.getIsPromotable());
                map.put("isMetric", m.getIsMetric());
                return map;
            }).toList();

            model.addAttribute("globalMeasurementsPool", flatMatrix);
        }
    }
}
