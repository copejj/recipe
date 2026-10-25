package com.braindribbler.recipe.validation;

import com.braindribbler.recipe.repository.auth.BlacklistedDomainRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DomainValidator implements ConstraintValidator<ValidDomain, String> {

    private final BlacklistedDomainRepository domainRepository;

    DomainValidator(BlacklistedDomainRepository domainRepository) {
        this.domainRepository = domainRepository;
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.isBlank()) {
            return true;
        }

        String lowerCaseEmail = email.toLowerCase().trim();

        // Extract the domain part after the @ symbol
        if (!lowerCaseEmail.contains("@")) {
            return false;
        }
        String domain = lowerCaseEmail.substring(lowerCaseEmail.indexOf("@") + 1);

        // Instantly verify against live database records
        return !domainRepository.existsByDomainName(domain);
    }
}
