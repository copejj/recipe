package com.braindribbler.recipe.repository.auth;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.braindribbler.recipe.domain.auth.LoginHistory;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    List<LoginHistory> findByUserAuthUsersAuthIdOrderByLoginAtDesc(Long usersAuthId);
}
