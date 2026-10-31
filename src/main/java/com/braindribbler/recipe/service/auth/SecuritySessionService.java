package com.braindribbler.recipe.service.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecuritySessionService {

    private final JdbcTemplate jdbcTemplate;

    public SecuritySessionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void logoutAllDevices(String email) {
        // Clears out every active device signature matching the logged-in email string
        jdbcTemplate.update(
            "DELETE FROM persistent_logins WHERE username = ?", 
            email
        );
    }
}
