package com.braindribbler.recipe.repository.auth;

import com.braindribbler.recipe.domain.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPublicId(UUID publicId);

    boolean existsByUserAuthEmail(String email); // Relationship traversal to search email in users_auth
}
