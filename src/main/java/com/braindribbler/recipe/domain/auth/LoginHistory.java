package com.braindribbler.recipe.domain.auth;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "login_history", schema = "public")
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "login_history_id")
    private Integer loginHistoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_auth_id", nullable = false)
    private UserAuth userAuth;

    @Column(name = "login_at", nullable = false)
    private OffsetDateTime loginAt = OffsetDateTime.now();

    @Column(name = "ip_address", nullable = false, columnDefinition = "text")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @Column(name = "is_successful", nullable = false)
    private boolean isSuccessful = true;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    @Column(name = "attempted_email", columnDefinition = "text")
    private String attemptedEmail;

    public LoginHistory() {
    }

    // Getters and Setters
    public Integer getLoginHistoryId() {
        return loginHistoryId;
    }

    public void setLoginHistoryId(Integer id) {
        this.loginHistoryId = id;
    }

    public UserAuth getUserAuth() {
        return userAuth;
    }

    public void setUserAuth(UserAuth userAuth) {
        this.userAuth = userAuth;
    }

    public OffsetDateTime getLoginAt() {
        return loginAt;
    }

    public void setLoginAt(OffsetDateTime loginAt) {
        this.loginAt = loginAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }

    public void setSuccessful(boolean isSuccessful) {
        this.isSuccessful = isSuccessful;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String reason) {
        this.failureReason = reason;
    }

    public String getAttemptedEmail() {
        return attemptedEmail;
    }

    public void setAttemptedEmail(String email) {
        this.attemptedEmail = email;
    }
}
