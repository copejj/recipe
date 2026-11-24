package com.braindribbler.recipe.repository.auth;

import com.braindribbler.recipe.domain.auth.UserMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface UserMessageRepository extends JpaRepository<UserMessage, Integer> {

    @Query(value = """
            SELECT * FROM user_messages
            WHERE (user_id IS NULL OR user_id = :userId)
              AND expires_at > :now
            ORDER BY (user_id IS NOT NULL) DESC, created_at DESC
            """, nativeQuery = true)
    List<UserMessage> findAllActiveMessagesForUser(@Param("userId") Integer userId, @Param("now") OffsetDateTime now);

    @Query(value = """
            SELECT * FROM user_messages
            WHERE (user_id IS NULL OR user_id = :userId)
              AND expires_at > :now
            ORDER BY created_at DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<UserMessage> findLatestActiveMessage(@Param("userId") Integer userId, @Param("now") OffsetDateTime now);

    @Query("SELECT m FROM UserMessage m LEFT JOIN FETCH m.user ORDER BY m.createdAt DESC")
    List<UserMessage> findAllWithUserOrderByCreatedAtDesc();

    long countByExpiresAtAfter(OffsetDateTime now);
}
