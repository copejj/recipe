package com.braindribbler.recipe.controller.admin;

import com.braindribbler.recipe.service.recipe.GroceryCategoryService; // ◄ Clean modular injection
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
        model.addAttribute("categories", groceryCategoryService.getAllCategoriesSorted());
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

    @PostMapping("/delete/{id}")
    public String removeCategoryRow(@PathVariable("id") Integer id) {
        try {
            groceryCategoryService.deleteCategoryById(id);
            return "redirect:/admin/categories?deleted=true";
        } catch (IllegalStateException e) {
            return "redirect:/admin/categories?error=inUse";
        }
    }
}
