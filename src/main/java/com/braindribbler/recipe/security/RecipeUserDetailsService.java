package com.braindribbler.recipe.security;

import com.braindribbler.recipe.domain.auth.UserAuth;
import com.braindribbler.recipe.repository.auth.LoginHistoryRepository;
import com.braindribbler.recipe.repository.auth.UserAuthRepository;
import com.braindribbler.recipe.security.exception.AuthenticationExceptions;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
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
        OffsetDateTime lockWindowSince = OffsetDateTime.now().minusHours(ipLockHours);
        long ipFailureCount = loginHistoryRepository.countRecentIpFailures(clientIp, lockWindowSince);

        if (ipFailureCount >= maxIpFailures) {
            throw new AuthenticationExceptions.IpBruteForceLockedException("IP temporary restriction active.");
        }

        UserAuth userAuth = userAuthRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No active account matching email: " + email));

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
