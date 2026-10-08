package com.braindribbler.recipe.domain.auth;

import java.time.OffsetDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "ip_overrides", schema = "public")
public class IpOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ip_override_id")
    private Integer ipOverrideId;

    @Column(name = "ip_address", nullable = false, unique = true, columnDefinition = "text")
    private String ipAddress;

    @Column(name = "override_until", nullable = false)
    private OffsetDateTime overrideUntil;

    public IpOverride() {
    }

    // Getters and Setters
    public Integer getIpOverrideId() {
        return ipOverrideId;
    }

    public void setIpOverrideId(Integer id) {
        this.ipOverrideId = id;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public OffsetDateTime getOverrideUntil() {
        return overrideUntil;
    }

    public void setOverrideUntil(OffsetDateTime overrideUntil) {
        this.overrideUntil = overrideUntil;
    }
}
