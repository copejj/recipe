package com.braindribbler.recipe.controller.admin;

import com.braindribbler.recipe.domain.recipe.GroceryCategory;
import com.braindribbler.recipe.dto.recipe.GroceryCategoryForm;
import com.braindribbler.recipe.service.recipe.GroceryCategoryService;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/recipes/metadata/grocery-categories")
@PreAuthorize("hasAuthority('can_moderate_recipe_metadata')")
public class GroceryCategoryController {

    private final GroceryCategoryService groceryCategoryService;

    public GroceryCategoryController(GroceryCategoryService groceryCategoryService) {
        this.groceryCategoryService = groceryCategoryService;
    }

    @GetMapping
    public String showCategoriesIndex(Model model) {
        List<GroceryCategory> categories = groceryCategoryService.getAllCategoriesSorted();

        model.addAttribute("formWrapper", new GroceryCategoryForm(categories));
        return "admin/recipes/metadata/grocery-categories/list";
    }

    @PostMapping("/add")
    public String createNewCategory(@RequestParam("categoryName") String name,
            @RequestParam("displayOrder") Integer displayOrder) {
        groceryCategoryService.createNewCategory(name, displayOrder);
        return "redirect:/admin/recipes/metadata/grocery-categories?created=true";
    }

    @PostMapping("/edit/{id}")
    public String modifyCategoryProperties(@PathVariable("id") Integer id,
            @RequestParam("categoryName") String name,
            @RequestParam("displayOrder") Integer displayOrder) {
        groceryCategoryService.updateCategory(id, name, displayOrder);
        return "redirect:/admin/recipes/metadata/grocery-categories?updated=true";
    }

    @PostMapping("/save-all")
    public String saveAllCategoriesUpdates(@ModelAttribute("formWrapper") GroceryCategoryForm form) {
        if (form != null && form.getItems() != null) {
            groceryCategoryService.saveAllCategories(form.getItems());
        }
        return "redirect:/admin/recipes/metadata/grocery-categories?updated=true";
    }

    @PostMapping("/delete/{id}")
    public String removeCategory(@PathVariable("id") Integer id) {
        try {
            groceryCategoryService.deleteCategory(id);
            return "redirect:/admin/recipes/metadata/grocery-categories?deleted=true";
        } catch (Exception e) {
            return "redirect:/admin/recipes/metadata/grocery-categories?error=inUse";
        }
    }
}
