package com.braindribbler.recipe.dto.recipe;

public class RecipeStats {
    private final Integer total;
    private final Integer publicCount;
    private final Integer sharedCount;
    private final Integer privateCount;
    private final Integer savedMeCount;
    private final Integer savedByMeCount;
    private long totalViews;
    private long totalShares;
    private long totalSharedViews;

    public RecipeStats(Integer total, Integer publicCount, Integer sharedCount, Integer privateCount) {
        this(total, publicCount, sharedCount, privateCount, 0, 0);
    }

    public RecipeStats(Integer total, Integer publicCount, Integer sharedCount, Integer privateCount, Integer savedMeCount) {
        this(total, publicCount, sharedCount, privateCount, savedMeCount, 0);
    }

    public RecipeStats(Integer total, Integer publicCount, Integer sharedCount, Integer privateCount, Integer savedMeCount, Integer savedByMeCount) {
        this.total = total;
        this.publicCount = publicCount;
        this.sharedCount = sharedCount;
        this.privateCount = privateCount;
        this.savedMeCount = savedMeCount;
        this.savedByMeCount = savedByMeCount;
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

    public Integer getSavedMeCount() {
        return savedMeCount;
    }

    public Integer getSavedByMeCount() {
        return savedByMeCount;
    }

    public long getTotalViews() {
        return totalViews;
    }

    public void setTotalViews(long totalViews) {
        this.totalViews = totalViews;
    }

    public long getTotalShares() {
        return totalShares;
    }

    public void setTotalShares(long totalShares) {
        this.totalShares = totalShares;
    }

    public long getTotalSharedViews() {
        return totalSharedViews;
    }

    public void setTotalSharedViews(long totalSharedViews) {
        this.totalSharedViews = totalSharedViews;
    }

}
