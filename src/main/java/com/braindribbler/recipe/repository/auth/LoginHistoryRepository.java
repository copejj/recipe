package com.braindribbler.recipe.repository.auth;

import com.braindribbler.recipe.domain.auth.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.List;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Integer> {

    List<LoginHistory> findByUserAuthUserAuthIdOrderByLoginAtDesc(Integer userAuthId);

    List<LoginHistory> findAllByOrderByLoginAtDesc();

    List<LoginHistory> findByUserAuthUserUserIdOrderByLoginAtDesc(Integer userId);

    @Query(value = "SELECT ua.email as actualEmail, lh.* " +
            "FROM public.login_history lh " +
            "LEFT JOIN public.users_auth ua USING (user_auth_id) " +
            "ORDER BY lh.login_at DESC", nativeQuery = true)
    List<Object[]> findAllLogsWithActualEmailNative();

    @Query(value = "SELECT ua.email as actualEmail, lh.* " +
            "FROM public.login_history lh " +
            "LEFT JOIN public.users_auth ua USING (user_auth_id) " +
            "WHERE ua.user_id = :userId " +
            "ORDER BY lh.login_at DESC", nativeQuery = true)
    List<Object[]> findLogsByUserIdWithActualEmailNative(@Param("userId") Integer userId);

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
