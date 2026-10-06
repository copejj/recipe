package com.braindribbler.recipe.repository.auth;

import com.braindribbler.recipe.domain.auth.UserAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UserAuthRepository extends JpaRepository<UserAuth, Integer> {

    @Query("SELECT ua FROM UserAuth ua " +
            "JOIN FETCH ua.user u " +
            "LEFT JOIN FETCH u.roles " +
            "WHERE LOWER(TRIM(ua.email)) = LOWER(TRIM(:email))")
    Optional<UserAuth> findByEmail(@Param("email") String email);
}
