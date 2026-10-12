package com.braindribbler.recipe.domain.auth;

import java.util.ArrayList;
import java.util.List;

public class MenuItem {
    private String label;
    private String url;
    private List<MenuItem> children = new ArrayList<>();

    // Simple constructor for flat links
    public MenuItem(String label, String url) {
        this.label = label;
        this.url = url;
    }

    // Constructor for dropdown groups
    public MenuItem(String label, List<MenuItem> children) {
        this.label = label;
        this.url = "#"; // Dropdowns typically don't navigate directly
        this.children = children;
    }

    // Getters and Setters
    public String getLabel() {
        return label;
    }

    public String getUrl() {
        return url;
    }

    public List<MenuItem> getChildren() {
        return children;
    }

    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }
}
