package com.braindribbler.recipe.repository.auth;

import com.braindribbler.recipe.domain.auth.UserMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface UserMessageRepository extends JpaRepository<UserMessage, Integer> {

    @Query(value = """
            SELECT * FROM user_messages
            WHERE (user_id IS NULL OR user_id = :userId)
              AND expires_at > :now
            ORDER BY created_at DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<UserMessage> findLatestActiveMessage(@Param("userId") Integer userId, @Param("now") OffsetDateTime now);
}
