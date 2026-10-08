package com.braindribbler.recipe.domain.auth;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "users_auth", schema = "public")
public class UserAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "users_auth_id")
    private Integer usersAuthId;

    @Column(nullable = false, unique = true, columnDefinition = "text")
    private String email;

    @Column(name = "password_hash", nullable = false, columnDefinition = "text")
    private String password;

    @Column(name = "password_changed_at", nullable = false)
    private OffsetDateTime passwordChangedAt = OffsetDateTime.now();

    @Column(name = "last_login")
    private OffsetDateTime lastLogin;

    @Column(name = "verification_token", columnDefinition = "uuid")
    private java.util.UUID verificationToken;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    public UserAuth() {
    }

    public Integer getUsersAuthId() {
        return usersAuthId;
    }

    public void setUsersAuthId(Integer usersAuthId) {
        this.usersAuthId = usersAuthId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public OffsetDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public void setPasswordChangedAt(OffsetDateTime passwordChangedAt) {
        this.passwordChangedAt = passwordChangedAt;
    }

    public OffsetDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(OffsetDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean isVerified) {
        this.isVerified = isVerified;
    }

    public UUID getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(UUID verificationToken) {
        this.verificationToken = verificationToken;
    }
}
