package com.braindribbler.recipe.controller.recipe;

import com.braindribbler.recipe.model.Recipe;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RecipeController {
    @GetMapping("/")
    public String getDashboard(Model model) {
        // Creating a mock Model object to test data passing
        Recipe sampleRecipe = new Recipe("Chocolate Chip Cookies", "Mix, bake, and enjoy!");

        // Passing the object to the Thymeleaf View layer
        model.addAttribute("recipe", sampleRecipe);
        return "index";
    }

    @GetMapping("/dashboard")
    public String showHome(Model model) {
        model.addAttribute("message", "Welcome! Your Argon2id authentication layer is 100% verified and operational.");
        return "recipes/dashboard";
    }

    @GetMapping("/search")
    public String showSearch(Model model) {
        model.addAttribute("message", "Search for your favorite recipes here!");
        return "recipes/search";
    }
}
