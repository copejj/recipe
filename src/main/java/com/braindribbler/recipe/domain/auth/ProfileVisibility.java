package com.braindribbler.recipe.domain.auth;

import jakarta.persistence.*;

@Entity
@Table(name = "profile_visibility", schema = "public")
public class ProfileVisibility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_visibility_id")
    private Integer profileVisibilityId;

    @Column(name = "visibility_name", nullable = false, unique = true, columnDefinition = "text")
    private String visibilityName;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    public ProfileVisibility() {
    }

    public Integer getProfileVisibilityId() {
        return profileVisibilityId;
    }

    public void setProfileVisibilityId(Integer profileVisibilityId) {
        this.profileVisibilityId = profileVisibilityId;
    }

    public String getVisibilityName() {
        return visibilityName;
    }

    public void setVisibilityName(String visibilityName) {
        this.visibilityName = visibilityName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
