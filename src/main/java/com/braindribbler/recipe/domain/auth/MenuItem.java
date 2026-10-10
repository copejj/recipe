package com.braindribbler.recipe.domain.auth;

public class MenuItem {
    private final String label;
    private final String url;

    public MenuItem(String label, String url) {
        this.label = label;
        this.url = url;
    }

    public String getLabel() {
        return label;
    }

    public String getUrl() {
        return url;
    }
}
