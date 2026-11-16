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
@RequestMapping("/admin/metadata/grocery/categories") 
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
        return "admin/metadata/grocery-categories/list";
    }

    @GetMapping({"/edit", "/edit/{id}"})
    public String showEditForm(@PathVariable(value = "id", required = false) Integer id, Model model) {
        GroceryCategory category = (id != null)
                ? groceryCategoryService.getAllCategoriesSorted().stream()
                    .filter(c -> c.getGroceryCategoryId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Grocery Category Id: " + id))
                : new GroceryCategory();

        model.addAttribute("category", category);
        return "admin/metadata/grocery-categories/edit";
    }

    @PostMapping({"/edit", "/edit/{id}"})
    public String saveOrUpdateCategory(@PathVariable(value = "id", required = false) Integer id,
                                       @RequestParam("categoryName") String name,
                                       @RequestParam("displayOrder") Integer displayOrder) {
        if (id != null) {
            groceryCategoryService.updateCategory(id, name, displayOrder);
            return "redirect:/admin/metadata/grocery/categories?updated=true";
        } else {
            groceryCategoryService.createNewCategory(name, displayOrder);
            return "redirect:/admin/metadata/grocery/categories?created=true";
        }
    }

    @PostMapping("/delete/{id}")
    public String removeCategory(@PathVariable("id") Integer id) {
        try {
            groceryCategoryService.deleteCategory(id);
            return "redirect:/admin/metadata/grocery/categories?deleted=true";
        } catch (Exception e) {
            return "redirect:/admin/metadata/grocery/categories?error=inUse";
        }
    }
}
