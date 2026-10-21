package com.braindribbler.recipe.dto.recipe;

import java.math.BigDecimal;

public class IngredientForm {
    private Integer ingredientId;
    private Integer measurementId;
    private BigDecimal amount;
    private String ingredientNote;
    private boolean isVerified;

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean isVerified) {
        this.isVerified = isVerified;
    }

    private Integer createdByUserId;

    public Integer getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Integer createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public IngredientForm() {
    }

    // Getters and Setters...
    public Integer getIngredientId() {
        return ingredientId;
    }

    public void setIngredientId(Integer ingredientId) {
        this.ingredientId = ingredientId;
    }

    public Integer getMeasurementId() {
        return measurementId;
    }

    public void setMeasurementId(Integer measurementId) {
        this.measurementId = measurementId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getIngredientNote() {
        return ingredientNote;
    }

    public void setIngredientNote(String ingredientNote) {
        this.ingredientNote = ingredientNote;
    }
}
