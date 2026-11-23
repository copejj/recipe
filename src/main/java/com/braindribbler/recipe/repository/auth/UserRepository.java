package com.braindribbler.recipe.repository.auth;

import com.braindribbler.recipe.domain.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByPublicId(UUID publicId);

    boolean existsByUserAuthEmail(String email);

    @Query("SELECT DISTINCT u FROM User u " +
        "JOIN FETCH u.userAuth ua ")
    List<User> findAllWithAuth();

    @Query("SELECT u FROM User u " +
        "JOIN FETCH u.userAuth ua " +
        "LEFT JOIN FETCH u.roles " +
        "WHERE LOWER(ua.email) = TRIM(LOWER(:email))")
    Optional<User> findByUserAuthEmail(@Param("email") String email);


    boolean existsByPublicId(UUID publicId);

    void deleteByPublicId(UUID publicId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.savedRecipes WHERE u.userId = :userId")
    Optional<User> findByIdWithSavedRecipes(@Param("userId") Integer userId);

    @Query(value = """
        SELECT u.* FROM users u
        WHERE u.disabled_at IS NULL
        AND u.user_id <> :currentUserId
        AND u.profile_visibility_id IN (1, 3)
        AND NOT EXISTS (
            SELECT 1 FROM user_blocks ub
            WHERE (ub.blocking_user_id = :currentUserId AND ub.blocked_user_id = u.user_id)
                OR (ub.blocking_user_id = u.user_id AND ub.blocked_user_id = :currentUserId)
        )
        ORDER BY LOWER(u.display_name) ASC
    """, nativeQuery = true)
    List<User> findShareableUsers(@Param("currentUserId") Integer currentUserId);
}
