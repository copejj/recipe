package com.braindribbler.recipe.security;

import com.braindribbler.recipe.domain.auth.UserAuth;
import com.braindribbler.recipe.repository.auth.LoginHistoryRepository;
import com.braindribbler.recipe.repository.auth.UserAuthRepository;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.security.ip-max-failures:20}")
    private int maxIpFailures;

    @Value("${app.security.ip-lock-hours:24}")
    private int ipLockHours;

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

        // 2. REFACTOR TIMELINE HOOK TO USE PROPERTY VALUE
        OffsetDateTime lockWindowSince = OffsetDateTime.now().minusHours(ipLockHours);
        long ipFailureCount = loginHistoryRepository.countRecentIpFailures(clientIp, lockWindowSince);

        // 3. EVALUATE DYNAMIC THRESHOLD LIMIT
        if (ipFailureCount >= maxIpFailures) {
            throw new LockedException("Your IP address has been temporarily blocked due to suspicious activity.");
        }

        UserAuth userAuth = userAuthRepository.findByEmail(email)
                .orElseThrow(
                        () -> new UsernameNotFoundException("No active account discovered matching email: " + email));

        if (userAuth.getUser() != null && userAuth.getUser().getDisabledAt() != null) {
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
        return xfHeader.split(",")[0].trim();
    }
}
