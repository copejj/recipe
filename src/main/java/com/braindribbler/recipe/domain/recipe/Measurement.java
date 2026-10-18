package com.braindribbler.recipe.domain.recipe;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "measurements")
public class Measurement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "measurement_id")
    private Integer measurementId;

    @Column(name = "unit_name", nullable = false, unique = true, columnDefinition = "text")
    private String unitName;

    @Column(name = "abbreviation", unique = true, columnDefinition = "text")
    private String abbreviation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_type_id", nullable = false)
    private BaseType baseType;

    @Column(name = "base_unit_equivalent", nullable = false, precision = 10, scale = 6)
    private BigDecimal baseUnitEquivalent;

    @Column(name = "scaling_rank", nullable = false)
    private Short scalingRank;

    // Inside your com.braindribbler.recipe.domain.recipe.Measurement class:

    @Column(name = "is_promotable", nullable = false)
    private Boolean isPromotable = true;

    @Column(name = "is_metric", nullable = false)
    private Boolean isMetric = false;

    @Column(name = "plural_name", columnDefinition = "text")
    private String pluralName;

    public Measurement() {
    }

    public Integer getMeasurementId() {
        return measurementId;
    }

    public void setMeasurementId(Integer measurementId) {
        this.measurementId = measurementId;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public BaseType getBaseType() {
        return baseType;
    }

    public void setBaseType(BaseType baseType) {
        this.baseType = baseType;
    }

    public Integer getBaseTypeId() {
        return this.baseType != null ? this.baseType.getBaseTypeId() : null;
    }

    public BigDecimal getBaseUnitEquivalent() {
        return baseUnitEquivalent;
    }

    public void setBaseUnitEquivalent(BigDecimal baseUnitEquivalent) {
        this.baseUnitEquivalent = baseUnitEquivalent;
    }

    public Short getScalingRank() {
        return scalingRank;
    }

    public void setScalingRank(Short scalingRank) {
        this.scalingRank = scalingRank;
    }

    public Boolean getIsPromotable() {
        return isPromotable;
    }

    public void setIsPromotable(Boolean isPromotable) {
        this.isPromotable = isPromotable;
    }

    public Boolean getIsMetric() {
        return isMetric;
    }

    public void setIsMetric(Boolean isMetric) {
        this.isMetric = isMetric;
    }

    public String getPluralName() {
        return pluralName;
    }

    public void setPluralName(String pluralName) {
        this.pluralName = pluralName;
    }
}
