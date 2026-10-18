package com.braindribbler.recipe.dto.recipe;

import com.braindribbler.recipe.domain.recipe.GroceryCategory;
import java.util.ArrayList;
import java.util.List;

public class GroceryCategoryForm {
    private List<GroceryCategory> items = new ArrayList<>();

    public GroceryCategoryForm() {
    }

    public GroceryCategoryForm(List<GroceryCategory> items) {
        this.items = items;
    }

    public List<GroceryCategory> getItems() {
        return items;
    }

    public void setItems(List<GroceryCategory> items) {
        this.items = items;
    }
}
