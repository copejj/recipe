package com.braindribbler.recipe.dto.recipe;

public class RecipeStats {
    private final Integer total;
    private final Integer publicCount;
    private final Integer sharedCount;
    private final Integer privateCount;

    public RecipeStats(Integer total, Integer publicCount, Integer sharedCount, Integer privateCount) {
        this.total = total;
        this.publicCount = publicCount;
        this.sharedCount = sharedCount;
        this.privateCount = privateCount;
    }

    public Integer getTotal() {
        return total;
    }

    public Integer getPublicCount() {
        return publicCount;
    }

    public Integer getSharedCount() {
        return sharedCount;
    }

    public Integer getPrivateCount() {
        return privateCount;
    }
}
