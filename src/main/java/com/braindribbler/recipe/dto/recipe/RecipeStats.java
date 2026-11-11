package com.braindribbler.recipe.dto.recipe;

public class RecipeStats {
    private final Integer total;
    private final Integer publicCount;
    private final Integer sharedCount;
    private final Integer privateCount;
    private final Integer totalSaves;

    public RecipeStats(Integer total, Integer publicCount, Integer sharedCount, Integer privateCount) {
        this(total, publicCount, sharedCount, privateCount, 0);
    }

    public RecipeStats(Integer total, Integer publicCount, Integer sharedCount, Integer privateCount, Integer totalSaves) {
        this.total = total;
        this.publicCount = publicCount;
        this.sharedCount = sharedCount;
        this.privateCount = privateCount;
        this.totalSaves = totalSaves;
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

    public Integer getTotalSaves() {
        return totalSaves;
    }
}
