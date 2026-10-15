package com.braindribbler.recipe.domain.recipe;

import jakarta.persistence.*;

@Entity
@Table(name = "grocery_category")
public class GroceryCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grocery_category_id")
    private Integer groceryCategoryId;

    @Column(name = "category_name", nullable = false, unique = true, columnDefinition = "text")
    private String categoryName;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0; // Default baseline value for sequence indexes

    public GroceryCategory() {
    }

    public Integer getGroceryCategoryId() {
        return groceryCategoryId;
    }

    public void setGroceryCategoryId(Integer groceryCategoryId) {
        this.groceryCategoryId = groceryCategoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
