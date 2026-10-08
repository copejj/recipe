package com.braindribbler.recipe.repository.auth;

import com.braindribbler.recipe.domain.auth.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.List;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Integer> {
    List<LoginHistory> findByUserAuthUsersAuthIdOrderByLoginAtDesc(Long usersAuthId);

    // Counts absolute malicious failed guessing sequences tied directly to a single
    // IP address
    @Query("SELECT COUNT(lh) FROM LoginHistory lh " +
            "WHERE lh.ipAddress = :ip " +
            "AND lh.isSuccessful = false " +
            "AND lh.loginAt > :sinceTime " +
            "AND NOT EXISTS (SELECT io FROM IpOverride io " +
            "                WHERE io.ipAddress = :ip " +
            "                AND io.overrideUntil > CURRENT_TIMESTAMP)")
    long countRecentIpFailures(@Param("ip") String ip, @Param("sinceTime") OffsetDateTime sinceTime);

}
