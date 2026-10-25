package com.braindribbler.recipe.repository.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.braindribbler.recipe.domain.auth.BlacklistItem;

public interface BlacklistRepository extends JpaRepository<BlacklistItem, Long> {
    Optional<BlacklistItem> findByPattern(String pattern);

    boolean existsByPattern(String pattern);
}
