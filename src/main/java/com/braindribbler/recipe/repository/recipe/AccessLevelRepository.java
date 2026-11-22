package com.braindribbler.recipe.repository.recipe;

import com.braindribbler.recipe.domain.recipe.AccessLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AccessLevelRepository extends JpaRepository<AccessLevel, Integer> {
    Optional<AccessLevel> findByAccessNameIgnoreCase(String accessName);
}
