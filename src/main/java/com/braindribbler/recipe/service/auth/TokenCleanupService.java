package com.braindribbler.recipe.service.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TokenCleanupService {

    private final JdbcTemplate jdbcTemplate;

    public TokenCleanupService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Automatically clears orphaned records at midnight daily
    @Scheduled(cron = "0 0 0 * * ?")
    public void purgeExpiredRememberMeTokens() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        
        int deletedRows = jdbcTemplate.update(
            "DELETE FROM persistent_logins WHERE last_used < ?", 
            cutoff
        );
        
        if (deletedRows > 0) {
            // Utilizes console routing or your active logger framework
            System.out.println("Purged " + deletedRows + " expired remember-me tokens from the database.");
        }
    }
}
