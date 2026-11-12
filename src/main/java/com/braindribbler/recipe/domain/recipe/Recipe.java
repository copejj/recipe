package com.braindribbler.recipe.domain.recipe;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.braindribbler.recipe.domain.auth.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "recipes")
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipe_id")
    private Integer recipeId;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @Column(name = "title")
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "servings")
    private Integer servings;

    @Column(name = "recipe_note", columnDefinition = "text")
    private String recipeNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_category_id")
    private RecipeCategory recipeCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_visibility_id", nullable = false)
    private RecipeVisibility recipeVisibility;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeStep> steps = new ArrayList<>();

    @ManyToMany(mappedBy = "savedRecipes", fetch = FetchType.LAZY)
    private Set<User> savedByUsers = new java.util.HashSet<>();

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @Column(name = "share_count", nullable = false)
    private Integer shareCount = 0;

    @Column(name = "shared_view_count", nullable = false)
    private Integer sharedViewCount = 0;

    @Column(name = "source_url", length=2048, nullable = true)
    private String sourceUrl;

    public Recipe() {
    }

    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public void setPublicId(UUID publicId) {
        this.publicId = publicId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getServings() {
        return servings;
    }

    public void setServings(Integer servings) {
        this.servings = servings;
    }

    public String getRecipeNote() {
        return recipeNote;
    }

    public void setRecipeNote(String recipeNote) {
        this.recipeNote = recipeNote;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public RecipeCategory getRecipeCategory() {
        return recipeCategory;
    }

    public void setRecipeCategory(RecipeCategory recipeCategory) {
        this.recipeCategory = recipeCategory;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public RecipeVisibility getRecipeVisibility() {
        return recipeVisibility;
    }

    public void setRecipeVisibility(RecipeVisibility recipeVisibility) {
        this.recipeVisibility = recipeVisibility;
    }

    public List<RecipeStep> getSteps() {
        return steps;
    }

    public void setSteps(List<RecipeStep> steps) {
        this.steps = steps;
    }

    public Set<User> getSavedByUsers() {
        return savedByUsers;
    }

    public void setSavedByUsers(Set<User> savedByUsers) {
        this.savedByUsers = savedByUsers;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getShareCount() {
        return shareCount;
    }

    public void setShareCount(Integer shareCount) {
        this.shareCount = shareCount;
    }

    public Integer getSharedViewCount() {
        return sharedViewCount;
    }

    public void setSharedViewCount(Integer sharedViewCount) {
        this.sharedViewCount = sharedViewCount;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        if (sourceUrl == null) {
            this.sourceUrl = null;
            return;
        }
        String trimmed = sourceUrl.trim();
        this.sourceUrl = trimmed.isEmpty() ? null : trimmed;
    }

    @jakarta.persistence.Transient
    public String getSourceDomain() {
        if (this.sourceUrl == null || this.sourceUrl.isBlank()) {
            return null;
        }
        try {
            String host = java.net.URI.create(this.sourceUrl).getHost();
            return (host != null && host.startsWith("www.")) ? host.substring(4) : host;
        } catch (Exception e) {
            return "Original Source";
        }
    }
}
