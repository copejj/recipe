package com.braindribbler.recipe.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestController {

    @GetMapping("/recipes/search")
    public String showSearchPage(Model model) {
        return "recipe-search"; // Points directly to recipe-search.html
    }
}
