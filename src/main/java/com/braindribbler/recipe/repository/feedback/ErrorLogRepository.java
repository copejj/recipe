package com.braindribbler.recipe.repository.feedback;

import com.braindribbler.recipe.domain.feedback.ErrorLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ErrorLogRepository extends JpaRepository<ErrorLog, Integer> {
    Optional<ErrorLog> findByReferenceCode(String referenceCode);

    @EntityGraph(attributePaths = {"user"})
    List<ErrorLog> findTop200ByOrderByCreatedAtDesc();

    @Modifying
    @Query("DELETE FROM ErrorLog e WHERE e.createdAt < :cutoff AND e.errorLogId NOT IN (SELECT f.errorLog.errorLogId FROM UserFeedback f WHERE f.errorLog IS NOT NULL)")
    int deleteUnreferencedOlderThan(@Param("cutoff") OffsetDateTime cutoff);

    @Modifying
    @Query("DELETE FROM ErrorLog e WHERE e.errorLogId NOT IN (SELECT f.errorLog.errorLogId FROM UserFeedback f WHERE f.errorLog IS NOT NULL)")
    int deleteAllUnreferenced();
}
