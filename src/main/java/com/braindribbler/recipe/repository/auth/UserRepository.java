package com.braindribbler.recipe.repository.auth;

import com.braindribbler.recipe.domain.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByPublicId(UUID publicId);

    @Query("SELECT u FROM User u " +
            "JOIN FETCH u.userAuth ua " +
            "LEFT JOIN FETCH u.roles " +
            "WHERE ua.email = :email")
    Optional<User> findByUserAuthEmail(@Param("email") String email);

    boolean existsByPublicId(UUID publicId);

    void deleteByPublicId(UUID publicId);
}
