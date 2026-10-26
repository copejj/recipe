package com.braindribbler.recipe.controller.admin;

import com.braindribbler.recipe.domain.recipe.GroceryCategory;
import com.braindribbler.recipe.dto.recipe.GroceryCategoryForm;
import com.braindribbler.recipe.service.recipe.GroceryCategoryService; // ◄ Clean modular injection

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/categories")
@PreAuthorize("hasAuthority('can_moderate_ingredients')")
public class GroceryCategoryManagementController {

    private final GroceryCategoryService groceryCategoryService;

    public GroceryCategoryManagementController(GroceryCategoryService groceryCategoryService) {
        this.groceryCategoryService = groceryCategoryService;
    }

    @GetMapping
    public String showCategoriesIndex(Model model) {
        List<GroceryCategory> categories = groceryCategoryService.getAllCategoriesSorted();

        model.addAttribute("formWrapper", new GroceryCategoryForm(categories));
        return "admin/categories/list";
    }

    @PostMapping("/add")
    public String createNewCategory(@RequestParam("categoryName") String name,
            @RequestParam("displayOrder") Integer displayOrder) {
        groceryCategoryService.createNewCategory(name, displayOrder);
        return "redirect:/admin/categories?created=true";
    }

    @PostMapping("/edit/{id}")
    public String modifyCategoryProperties(@PathVariable("id") Integer id,
            @RequestParam("categoryName") String name,
            @RequestParam("displayOrder") Integer displayOrder) {
        groceryCategoryService.updateCategory(id, name, displayOrder);
        return "redirect:/admin/categories?updated=true";
    }

    @PostMapping("/save-all")
    public String saveAllCategoriesUpdates(@ModelAttribute("formWrapper") GroceryCategoryForm form) {
        if (form != null && form.getItems() != null) {
            groceryCategoryService.saveAllCategories(form.getItems());
        }
        return "redirect:/admin/categories?updated=true";
    }

    @PostMapping("/delete/{id}")
    public String removeCategory(@PathVariable("id") Integer id) {
        try {
            groceryCategoryService.deleteCategory(id);
            return "redirect:/admin/categories?deleted=true";
        } catch (Exception e) {
            // If an ingredient uses this aisle, Postgres will throw a foreign key error
            // caught here
            return "redirect:/admin/categories?error=inUse";
        }
    }
}
