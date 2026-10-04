package com.braindribbler.recipe.domain.recipe;

import jakarta.persistence.*;

@Entity
@Table(name = "grocery_category", schema = "public")
public class GroceryCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grocery_category_id")
    private Integer groceryCategoryId;

    @Column(name = "category_name", nullable = false, unique = true)
    private String categoryName;

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
}
