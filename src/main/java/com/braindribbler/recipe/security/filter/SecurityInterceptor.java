package com.braindribbler.recipe.security.filter;

import com.braindribbler.recipe.domain.auth.BlacklistItem;
import com.braindribbler.recipe.service.auth.BlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.io.IOException;
import java.util.List;

@Component
public class SecurityInterceptor implements HandlerInterceptor {

    private final BlacklistService blacklistService;

    public SecurityInterceptor(BlacklistService blacklistService) {
        this.blacklistService = blacklistService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 1. Extract the raw connection IP hitting your server
        String clientIp = request.getRemoteAddr();

        // Handle proxy server forwarding setups if you run behind Cloudflare or an
        // Nginx router downstream
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            clientIp = forwardedFor.split(",")[0].trim();
        }

        // 2. Fetch active firewall parameter listings from your PostgreSQL repository
        // cache
        List<BlacklistItem> activeBans = blacklistService.getAllItems();

        for (BlacklistItem ban : activeBans) {
            String type = ban.getType();
            String pattern = ban.getPattern();

            // 3. Evaluate exact IP rules
            if ("IP".equalsIgnoreCase(type) && clientIp.equalsIgnoreCase(pattern)) {
                triggerAccessBlock(response, clientIp, ban.getReason());
                return false; // Terminates execution paths immediately
            }

            // 4. Evaluate network Subnet block CIDR rules (e.g. 198.46.154.0/23)
            if ("SUBNET".equalsIgnoreCase(type) && isIpInSubnet(clientIp, pattern)) {
                triggerAccessBlock(response, clientIp, ban.getReason());
                return false;
            }
        }

        return true; // Proceed smoothly for legitimate users
    }

    private void triggerAccessBlock(HttpServletResponse response, String ip, String reason) throws IOException {
        // Send a clean, un-exploitable 403 Forbidden status code straight back to the
        // machine
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("text/plain");
        response.getWriter()
                .write("Access Denied: Your IP address (" + ip + ") has been administrative blocked. Reason: "
                        + (reason != null ? reason : "Security Threat Policy Violation."));
    }

    private boolean isIpInSubnet(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2)
                return false;

            long ipAddress = ipToLong(ip);
            long subnetAddress = ipToLong(parts[0]);
            int bits = Integer.parseInt(parts[1]);

            long mask = -1L << (32 - bits);
            return (ipAddress & mask) == (subnetAddress & mask);
        } catch (Exception e) {
            return false; // Quietly ignore formatting anomalies to prevent runtime crashes
        }
    }

    private long ipToLong(String ipAddress) {
        String[] ipAddressInArray = ipAddress.split("\\.");
        long result = 0;
        for (int i = 0; i < ipAddressInArray.length; i++) {
            int power = 3 - i;
            int ip = Integer.parseInt(ipAddressInArray[i]);
            result += ip * Math.pow(256, power);
        }
        return result;
    }
}
