package com.braindribbler.recipe.controller.admin;

import com.braindribbler.recipe.domain.recipe.RecipeCategory;
import com.braindribbler.recipe.service.recipe.RecipeCategoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/recipes/metadata/categories")
@PreAuthorize("hasAuthority('can_moderate_recipe_metadata')")
public class RecipeCategoryController {

    private final RecipeCategoryService recipeCategoryService;

    public RecipeCategoryController(RecipeCategoryService recipeCategoryService) {
        this.recipeCategoryService = recipeCategoryService;
    }

    // 1. Render Admin Dashboard List
    @GetMapping
    public String showCategoryDashboard(Model model) {
        model.addAttribute("categories", recipeCategoryService.getAllCategories());
        model.addAttribute("newCategory", new RecipeCategory()); 
        return "admin/recipes/metadata/categories/dashboard";
    }

    // 2. Process Add Category Form
    @PostMapping("/add")
    public String addNewCategory(@ModelAttribute("newCategory") RecipeCategory category) {
        if (category.getName() != null && !category.getName().trim().isEmpty()) {
            recipeCategoryService.saveCategory(category);
        }
        return "redirect:/admin/recipes/metadata/categories";
    }

    // 3. Render Edit View Panel
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        RecipeCategory category = recipeCategoryService.getCategoryById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Category Id:" + id));
        model.addAttribute("category", category);
        return "admin/recipes/metadata/categories/edit";
    }

    // 4. Process Modification Updates
    @PostMapping("/update/{id}")
    public String updateCategory(@PathVariable("id") Integer id, @ModelAttribute("category") RecipeCategory category) {
        category.setRecipeCategoryId(id);
        recipeCategoryService.saveCategory(category);
        return "redirect:/admin/recipes/metadata/categories";
    }

    // 5. Delete Taxonomy Node
    @GetMapping("/delete/{id}")
    public String deleteCategory(@PathVariable("id") Integer id) {
        recipeCategoryService.deleteCategoryById(id);
        return "redirect:/admin/recipes/metadata/categories";
    }
}
