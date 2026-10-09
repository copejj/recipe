package com.braindribbler.recipe.repository.auth;

import com.braindribbler.recipe.domain.auth.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.OffsetDateTime;
import java.util.List;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Integer> {

    List<LoginHistory> findByUserAuthUserAuthIdOrderByLoginAtDesc(Integer userAuthId);

    List<LoginHistory> findAllByOrderByLoginAtDesc();

    List<LoginHistory> findByUserAuthUserUserIdOrderByLoginAtDesc(Integer userId);

    @Query("SELECT lh FROM LoginHistory lh LEFT JOIN FETCH lh.userAuth ua ORDER BY lh.loginAt DESC")
    List<LoginHistory> findAllLogsWithUserAuth();

    @Query("SELECT COUNT(lh) FROM LoginHistory lh " +
            "WHERE lh.ipAddress = :ip " +
            "AND lh.isSuccessful = false " +
            "AND lh.loginAt > :sinceTime " +
            "AND NOT EXISTS (SELECT io FROM IpOverride io " +
            "                WHERE io.ipAddress = :ip " +
            "                AND io.overrideUntil > CURRENT_TIMESTAMP)")
    long countRecentIpFailures(String ip, OffsetDateTime sinceTime);
}
