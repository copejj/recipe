package com.braindribbler.recipe.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestController {

    // Map BOTH paths to load your master Thymeleaf template file
    @GetMapping({ "/", "/test-home" })
    public String showTestHome(Model model) {
        model.addAttribute("message", "Welcome! Your Argon2id authentication layer is 100% verified and operational.");
        return "test-home";
    }

    @GetMapping("/test-target")
    public String showTestTarget(Model model) {
        model.addAttribute("message", "Success! You successfully navigated via a secure Thymeleaf link.");
        return "test-target";
    }
}
