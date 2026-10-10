package com.braindribbler.recipe.service.auth;

import com.braindribbler.recipe.domain.auth.Role;
import com.braindribbler.recipe.repository.auth.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoleRankCache implements CommandLineRunner {

    private final RoleRepository roleRepository;

    // Thread-safe in-memory cache matrix map
    private final Map<String, Integer> cache = new ConcurrentHashMap<>();

    public RoleRankCache(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * Spring Boot automatically fires this execution method exactly once
     * immediately after the application context completes startup.
     */
    @Override
    public void run(String... args) throws Exception {
        refreshCache();
    }

    /**
     * Populates or refreshes the cache map directly from the PostgreSQL table.
     */
    public void refreshCache() {
        cache.clear();
        roleRepository.findAll().forEach(role -> {
            if (role.getRoleName() != null) {
                int rank = role.getRoleRank() != null ? role.getRoleRank() : 0;
                cache.put(role.getRoleName(), rank);
            }
        });
    }

    /**
     * Fast, local in-memory lookup. Zero database roundtrips.
     */
    public int getRankValue(String roleName) {
        if (roleName == null) {
            return 0;
        }
        return cache.getOrDefault(roleName, 0);
    }
}
