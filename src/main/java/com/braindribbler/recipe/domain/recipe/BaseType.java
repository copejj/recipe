package com.braindribbler.recipe.domain.recipe;

import jakarta.persistence.*;

@Entity
@Table(name = "base_type", schema = "public")
public class BaseType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "base_type_id")
    private Integer baseTypeId;

    @Column(name = "type_name", nullable = false, unique = true)
    private String typeName;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    public BaseType() {
    }

    public Integer getBaseTypeId() {
        return baseTypeId;
    }

    public void setBaseTypeId(Integer baseTypeId) {
        this.baseTypeId = baseTypeId;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
