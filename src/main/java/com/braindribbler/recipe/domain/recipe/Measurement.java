package com.braindribbler.recipe.domain.recipe;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "measurements", schema = "public")
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
}
