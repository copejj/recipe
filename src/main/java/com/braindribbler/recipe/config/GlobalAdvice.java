package com.braindribbler.recipe.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.GitProperties;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

@ControllerAdvice
public class GlobalAdvice {

    @Autowired(required = false)
    private GitProperties gitProperties;

    @ModelAttribute
    public void addBuildVersionToModel(Model model) {
        if (gitProperties != null) {
            String tag = gitProperties.get("closest.tag.name");
            String branch = gitProperties.getBranch();
            String fullHash = gitProperties.get("commit.id");
            String shortHash = gitProperties.getShortCommitId();

            String displayVersion;

            // Handle detached HEAD (common in CI/CD production pipelines)
            if (branch != null && branch.equals(fullHash)) {
                displayVersion = (tag != null && !tag.isEmpty()) ? tag : shortHash;
            } else {
                // Fallback to active working branch names (common for local development)
                displayVersion = (branch != null) ? branch : "unknown";
            }

            model.addAttribute("gitBranch", displayVersion);
            model.addAttribute("gitHash", shortHash);
            model.addAttribute("showGitInfo", true);
        } else {
            model.addAttribute("showGitInfo", false);
        }
    }
}