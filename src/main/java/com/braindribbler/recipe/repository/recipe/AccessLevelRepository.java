package com.braindribbler.recipe.repository.recipe;

import org.springframework.data.jpa.repository.JpaRepository;

import com.braindribbler.recipe.domain.recipe.AccessLevel;

public interface AccessLevelRepository extends JpaRepository<AccessLevel, Integer> {
}
