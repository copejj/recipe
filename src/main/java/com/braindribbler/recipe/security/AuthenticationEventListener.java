package com.braindribbler.recipe.security;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.braindribbler.recipe.domain.auth.LoginHistory;
import com.braindribbler.recipe.domain.auth.UserAuth;
import com.braindribbler.recipe.repository.auth.LoginHistoryRepository;
import com.braindribbler.recipe.repository.auth.UserAuthRepository;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class AuthenticationEventListener {

    private final UserAuthRepository userAuthRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final HttpServletRequest request;

    public AuthenticationEventListener(UserAuthRepository userAuthRepository,
            LoginHistoryRepository loginHistoryRepository,
            HttpServletRequest request) {
        this.userAuthRepository = userAuthRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.request = request;
    }

    @EventListener
    @Transactional
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String email = event.getAuthentication().getName();

        userAuthRepository.findByEmail(email).ifPresent(userAuth -> {
            OffsetDateTime now = OffsetDateTime.now();

            userAuth.setLastLogin(now);
            userAuthRepository.save(userAuth);

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
     * Catching AbstractAuthenticationFailureEvent handles ALL failures, including
     * bad credentials, locked states, and our newly added Inactive user exceptions.
     */
    @EventListener
    @Transactional
    public void handleAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String email = event.getAuthentication().getName();
        Exception exception = event.getException();

        String failureReason = "UNKNOWN_FAILURE";

        if (exception instanceof BadCredentialsException) {
            failureReason = "BAD_CREDENTIALS";
        } else if (exception instanceof LockedException) {
            failureReason = "IP_LOCKED";
        } else if (exception instanceof DisabledException) {
            // Check if the user entity has a disabledAt timestamp set to evaluate context
            Optional<UserAuth> userAuthOpt = userAuthRepository.findByEmail(email);
            if (userAuthOpt.isPresent() && userAuthOpt.get().getUser() != null
                    && userAuthOpt.get().getUser().getDisabledAt() != null) {
                failureReason = "ACCOUNT_INACTIVE";
            } else {
                failureReason = "ACCOUNT_UNVERIFIED";
            }
        }

        logFailure(email, failureReason);
    }

    private void logFailure(String email, String reason) {
        LoginHistory history = new LoginHistory();
        history.setLoginAt(OffsetDateTime.now());
        history.setSuccessful(false);
        history.setFailureReason(reason);
        history.setIpAddress(getClientIpAddress());
        history.setUserAgent(request.getHeader("User-Agent"));
        history.setAttemptedEmail(email);

        userAuthRepository.findByEmail(email).ifPresentOrElse(
                userAuth -> {
                    history.setUserAuth(userAuth);
                    loginHistoryRepository.save(history);
                },
                () -> {
                    history.setUserAuth(null);
                    history.setFailureReason("UNKNOWN_USER_" + reason);
                    loginHistoryRepository.save(history);
                });
    }

    private String getClientIpAddress() {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
