package com.braindribbler.recipe.domain.recipe;

import jakarta.persistence.*;

@Entity
@Table(name = "access_levels", schema = "public")
public class AccessLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "access_level_id")
    private Integer accessLevelId;

    @Column(name = "access_name", nullable = false, unique = true)
    private String accessName;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    public AccessLevel() {
    }

    public Integer getAccessLevelId() {
        return accessLevelId;
    }

    public void setAccessLevelId(Integer accessLevelId) {
        this.accessLevelId = accessLevelId;
    }

    public String getAccessName() {
        return accessName;
    }

    public void setAccessName(String accessName) {
        this.accessName = accessName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
