package com.braindribbler.recipe.domain.recipe;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recipe_categories")
public class RecipeCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipe_category_id")
    private Integer recipeCategoryId;

    @Column(name = "name", nullable = false, unique = true, columnDefinition = "text")
    private String name;

    @OneToMany(mappedBy = "recipeCategory", cascade = CascadeType.ALL)
    private List<Recipe> recipes = new ArrayList<>();

    public RecipeCategory() {
    }

    public Integer getRecipeCategoryId() {
        return recipeCategoryId;
    }

    public void setRecipeCategoryId(Integer recipeCategoryId) {
        this.recipeCategoryId = recipeCategoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Recipe> getRecipes() {
        return recipes;
    }

    public void setRecipes(List<Recipe> recipes) {
        this.recipes = recipes;
    }
}
