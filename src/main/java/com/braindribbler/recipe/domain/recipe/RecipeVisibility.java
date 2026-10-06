package com.braindribbler.recipe.domain.recipe;

import jakarta.persistence.*;

@Entity
@Table(name = "recipe_visibility", schema = "public")
public class RecipeVisibility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipe_visibility_id")
    private Integer recipeVisibilityId;

    @Column(name = "visibility_name", nullable = false, unique = true, columnDefinition = "text")
    private String visibilityName;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    public RecipeVisibility() {
    }

    public Integer getRecipeVisibilityId() {
        return recipeVisibilityId;
    }

    public void setRecipeVisibilityId(Integer recipeVisibilityId) {
        this.recipeVisibilityId = recipeVisibilityId;
    }

    public String getVisibilityName() {
        return visibilityName;
    }

    public void setVisibilityName(String visibilityName) {
        this.visibilityName = visibilityName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
