package com.braindribbler.recipe.controller.admin;

import com.braindribbler.recipe.domain.recipe.RecipeCategory;
import com.braindribbler.recipe.service.recipe.RecipeCategoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/metadata/recipes/categories") // ◄ EXACT FOLDER MATCH
@PreAuthorize("hasAuthority('can_moderate_recipe_metadata')")
public class RecipeCategoryController {

    private final RecipeCategoryService recipeCategoryService;

    public RecipeCategoryController(RecipeCategoryService recipeCategoryService) {
        this.recipeCategoryService = recipeCategoryService;
    }

    @GetMapping
    public String showCategoryDashboard(Model model) {
        model.addAttribute("categories", recipeCategoryService.getAllCategories());
        model.addAttribute("newCategory", new RecipeCategory()); 
        return "admin/metadata/recipes/categories/list";
    }

    @PostMapping("/add")
    public String addNewCategory(@ModelAttribute("newCategory") RecipeCategory category) {
        if (category.getName() != null && !category.getName().trim().isEmpty()) {
            recipeCategoryService.saveCategory(category);
        }
        return "redirect:/admin/metadata/recipes/categories";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        RecipeCategory category = recipeCategoryService.getCategoryById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Category Id:" + id));
        model.addAttribute("category", category);
        return "admin/metadata/recipes/categories/edit";
    }

    @PostMapping("/update/{id}")
    public String updateCategory(@PathVariable("id") Integer id, @ModelAttribute("category") RecipeCategory category) {
        category.setRecipeCategoryId(id);
        recipeCategoryService.saveCategory(category);
        return "redirect:/admin/metadata/recipes/categories";
    }

    @GetMapping("/delete/{id}")
    public String deleteCategory(@PathVariable("id") Integer id) {
        recipeCategoryService.deleteCategoryById(id);
        return "redirect:/admin/metadata/recipes/categories";
    }
}
