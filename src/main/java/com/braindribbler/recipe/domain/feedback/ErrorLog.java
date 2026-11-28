package com.braindribbler.recipe.domain.feedback;

import com.braindribbler.recipe.domain.auth.User;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "error_logs")
public class ErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "error_log_id")
    private Integer errorLogId;

    @Column(name = "reference_code", nullable = false, unique = true, length = 16)
    private String referenceCode;

    @Column(name = "status_code", nullable = false)
    private Integer statusCode;

    @Column(name = "request_uri", columnDefinition = "text")
    private String requestUri;

    @Column(name = "http_method", length = 16)
    private String httpMethod;

    @Column(name = "exception_class", columnDefinition = "text")
    private String exceptionClass;

    @Column(name = "exception_message", columnDefinition = "text")
    private String exceptionMessage;

    @Column(name = "stack_trace", columnDefinition = "text")
    private String stackTrace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public ErrorLog() {}

    public Integer getErrorLogId() { return errorLogId; }
    public void setErrorLogId(Integer errorLogId) { this.errorLogId = errorLogId; }

    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }

    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }

    public String getRequestUri() { return requestUri; }
    public void setRequestUri(String requestUri) { this.requestUri = requestUri; }

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

    public String getExceptionClass() { return exceptionClass; }
    public void setExceptionClass(String exceptionClass) { this.exceptionClass = exceptionClass; }

    public String getExceptionMessage() { return exceptionMessage; }
    public void setExceptionMessage(String exceptionMessage) { this.exceptionMessage = exceptionMessage; }

    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
