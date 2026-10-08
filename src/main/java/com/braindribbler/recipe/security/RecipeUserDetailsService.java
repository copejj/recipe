package com.braindribbler.recipe.security;

import com.braindribbler.recipe.domain.auth.UserAuth;
import com.braindribbler.recipe.repository.auth.LoginHistoryRepository;
import com.braindribbler.recipe.repository.auth.UserAuthRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;

@Service
public class RecipeUserDetailsService implements UserDetailsService {

    private final UserAuthRepository userAuthRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final HttpServletRequest request;

    public RecipeUserDetailsService(UserAuthRepository userAuthRepository,
            LoginHistoryRepository loginHistoryRepository,
            HttpServletRequest request) {
        this.userAuthRepository = userAuthRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.request = request;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String clientIp = getClientIpAddress();

        // 1. HARDENED IP LOCKOUT: Block the IP for 24 hours if it has 20+ total
        // failures
        OffsetDateTime oneDayAgo = OffsetDateTime.now().minusDays(1);
        long ipFailureCount = loginHistoryRepository.countRecentIpFailures(clientIp, oneDayAgo);

        if (ipFailureCount >= 20) {
            throw new LockedException(
                    "Your IP address has been temporarily blocked for 24 hours due to suspicious activity.");
        }

        // 2. FETCH THE USER ACCOUNT
        UserAuth userAuth = userAuthRepository.findByEmail(email)
                .orElseThrow(
                        () -> new UsernameNotFoundException("No active account discovered matching email: " + email));

        // 3. ADMINISTRATIVE BAN: Check if disabled_at is populated on the User entity
        if (userAuth.getUser() != null && userAuth.getUser().getDisabledAt() != null) {
            // If the disabled_at timestamp is in the past, block access instantly
            if (userAuth.getUser().getDisabledAt().isBefore(OffsetDateTime.now())) {
                throw new DisabledException("This account has been administratively disabled.");
            }
        }

        return new RecipeUserDetails(userAuth);
    }

    private String getClientIpAddress() {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim(); // Extract only the first client IP address
    }
}
