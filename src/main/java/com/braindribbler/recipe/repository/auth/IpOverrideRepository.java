package com.braindribbler.recipe.repository.auth;

import com.braindribbler.recipe.domain.auth.IpOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface IpOverrideRepository extends JpaRepository<IpOverride, Integer> {
    Optional<IpOverride> findByIpAddress(String ipAddress);
}
