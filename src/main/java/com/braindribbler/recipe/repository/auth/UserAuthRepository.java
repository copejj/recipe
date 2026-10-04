package com.braindribbler.recipe.repository.auth;

import com.braindribbler.recipe.domain.auth.UserAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserAuthRepository extends JpaRepository<UserAuth, Long> {
    Optional<UserAuth> findByEmail(String email);
}
