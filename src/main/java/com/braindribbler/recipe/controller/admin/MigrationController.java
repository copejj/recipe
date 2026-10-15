package com.braindribbler.recipe.controller.admin;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/migrations")
@PreAuthorize("hasAuthority('can_manage_migrations')")
public class MigrationController {

    private final JdbcTemplate jdbcTemplate;

    public MigrationController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public String showMigrationHistory(Model model) {
        // 1. Dynamic Meta Check: Query the PostgreSQL catalog to verify table existence
        String checkTableSql = "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'schema_migrations')";
        Boolean tableExists = jdbcTemplate.queryForObject(checkTableSql, Boolean.class);

        if (Boolean.TRUE.equals(tableExists)) {
            // 2. Fetch rows safely as raw Map arrays to avoid hardcoded static entities
            String querySql = "SELECT id, filename, status, started_at, ended_at, error_message FROM public.schema_migrations ORDER BY id DESC";
            List<Map<String, Object>> migrationRecords = jdbcTemplate.queryForList(querySql);

            model.addAttribute("tableExists", true);
            model.addAttribute("migrations", migrationRecords);
        } else {
            // 3. Graceful Local Fallback State
            model.addAttribute("tableExists", false);
        }

        return "admin/migrations/history"; // Points securely to templates/admin/migration-list.html
    }
}
