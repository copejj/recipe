package com.braindribbler.recipe.repository.auth;

import com.braindribbler.recipe.domain.auth.ProfileVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProfileVisibilityRepository extends JpaRepository<ProfileVisibility, Integer> {
    Optional<ProfileVisibility> findByVisibilityName(String visibilityName);
}
