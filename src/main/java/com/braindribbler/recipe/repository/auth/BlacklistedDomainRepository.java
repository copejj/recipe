package com.braindribbler.recipe.repository.auth;

import com.braindribbler.recipe.domain.auth.BlacklistedDomain;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlacklistedDomainRepository extends JpaRepository<BlacklistedDomain, Long> {
    boolean existsByDomainName(String domainName);
}
