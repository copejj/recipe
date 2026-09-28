package com.braindribbler.recipe.controller;

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
}

