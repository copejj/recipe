package com.braindribbler.recipe.repository.auth;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.braindribbler.recipe.domain.auth.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    List<UserRole> findByUserUserId(Long userId);
}
