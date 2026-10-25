package com.braindribbler.recipe.validation;

import com.braindribbler.recipe.repository.auth.BlacklistRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DomainValidator implements ConstraintValidator<ValidDomain, String> {

    private final BlacklistRepository blacklistRepository;

    // Updated constructor variable naming scheme to match your generic structure
    DomainValidator(BlacklistRepository blacklistRepository) {
        this.blacklistRepository = blacklistRepository;
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.isBlank()) {
            return true;
        }

        String lowerCaseEmail = email.toLowerCase().trim();

        // Basic syntax routing gatekeeper check
        if (!lowerCaseEmail.contains("@")) {
            return false;
        }

        // Extract the domain part after the @ symbol
        String domain = lowerCaseEmail.substring(lowerCaseEmail.indexOf("@") + 1).trim();

        // Instantly verify if the extracted domain pattern is blacklisted
        // This invokes your refactored existsByPattern() method cleanly
        return !blacklistRepository.existsByPattern(domain);
    }
}
