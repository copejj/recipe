package com.braindribbler.recipe.security;

import java.time.OffsetDateTime;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationFailureDisabledEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.braindribbler.recipe.domain.auth.LoginHistory;
import com.braindribbler.recipe.repository.auth.LoginHistoryRepository;
import com.braindribbler.recipe.repository.auth.UserAuthRepository;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class AuthenticationEventListener {

    private final UserAuthRepository userAuthRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final HttpServletRequest request; // Natively injects incoming request details

    public AuthenticationEventListener(UserAuthRepository userAuthRepository,
            LoginHistoryRepository loginHistoryRepository,
            HttpServletRequest request) {
        this.userAuthRepository = userAuthRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.request = request;
    }

    /**
     * Intercepts successful authentications.
     * Updates users_auth.last_login and writes a success entry to login_history.
     */
    @EventListener
    @Transactional
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String email = event.getAuthentication().getName();

        userAuthRepository.findByEmail(email).ifPresent(userAuth -> {
            OffsetDateTime now = OffsetDateTime.now();

            // 1. Update the last_login column on the users_auth table
            userAuth.setLastLogin(now);
            userAuthRepository.save(userAuth);

            // 2. Track successful log inside login_history
            LoginHistory history = new LoginHistory();
            history.setUserAuth(userAuth);
            history.setLoginAt(now);
            history.setSuccessful(true);
            history.setIpAddress(getClientIpAddress());
            history.setUserAgent(request.getHeader("User-Agent"));

            loginHistoryRepository.save(history);
        });
    }

    /**
     * Intercepts failed log ins due to wrong password or nonexistent emails.
     */
    @EventListener
    @Transactional
    public void handleAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        String email = event.getAuthentication().getName();
        logFailure(email, "BAD_CREDENTIALS");
    }

    /**
     * Intercepts failed log ins due to unverified/disabled user accounts.
     */
    @EventListener
    @Transactional
    public void handleAuthenticationFailureDisabled(AuthenticationFailureDisabledEvent event) {
        String email = event.getAuthentication().getName();
        logFailure(email, "ACCOUNT_UNVERIFIED");
    }

    private void logFailure(String email, String reason) {
        LoginHistory history = new LoginHistory();
        history.setLoginAt(OffsetDateTime.now());
        history.setSuccessful(false);
        history.setFailureReason(reason);
        history.setIpAddress(getClientIpAddress());
        history.setUserAgent(request.getHeader("User-Agent"));

        // Store the raw string typed into the username box for forensic review
        history.setAttemptedEmail(email);

        // Look up if the target user actually exists
        userAuthRepository.findByEmail(email).ifPresentOrElse(
                userAuth -> {
                    // A: Real User, wrong password or unverified
                    history.setUserAuth(userAuth);
                    loginHistoryRepository.save(history);
                },
                () -> {
                    // B: Fake User! We still save the row to catch malicious probing bots
                    history.setUserAuth(null); // Left null due to our DB schema alteration
                    history.setFailureReason("UNKNOWN_USER_" + reason);
                    loginHistoryRepository.save(history);

                    System.out.println("SECURITY ALERT: Blind authentication probe caught against: " + email);
                });
    }

    /**
     * Helper tool to accurately extract the true client IP address,
     * even if your Linux server sits behind an Nginx reverse-proxy down the line.
     */
    private String getClientIpAddress() {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
