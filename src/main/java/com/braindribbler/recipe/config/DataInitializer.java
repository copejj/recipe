package com.braindribbler.recipe.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.braindribbler.recipe.domain.auth.Role;
import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.domain.auth.UserAuth;
import com.braindribbler.recipe.repository.auth.ProfileVisibilityRepository;
import com.braindribbler.recipe.repository.auth.RoleRepository; // Added
import com.braindribbler.recipe.repository.auth.UserAuthRepository;
import com.braindribbler.recipe.repository.auth.UserRepository;
import com.braindribbler.recipe.service.auth.UserRegistrationService;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final UserRegistrationService userRegistrationService;
    private final RoleRepository roleRepository; // 1. Added repository field

    // 2. Added RoleRepository as a parameter to inject it here
    public DataInitializer(UserRepository userRepository,
            UserAuthRepository userAuthRepository,
            ProfileVisibilityRepository profileVisibilityRepository,
            UserRegistrationService userRegistrationService,
            RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.userAuthRepository = userAuthRepository;
        this.userRegistrationService = userRegistrationService;
        this.roleRepository = roleRepository;
    }

    @Value("${app.seed.admin-email:#{null}}")
    private String adminEmail;
    @Value("${app.seed.admin-password:#{null}}")
    private String adminPassword;
    @Value("${app.seed.admin-first-name:#{null}}")
    private String adminFirstName;
    @Value("${app.seed.admin-last-name:#{null}}")
    private String adminLastName;
    @Value("${app.seed.admin-display-name:#{null}}")
    private String adminDisplayName;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            if (isAnyFieldNullOrBlank()) {
                System.out.println("Database is empty, but seeding was skipped due to missing config keys.");
                return;
            }

            System.out.println("Database is empty! Seeding primary super admin account...");

            Role adminRole = roleRepository.findByRoleName("ROLE_SUPER_ADMIN")
                    .orElseThrow(() -> new IllegalStateException("Required ROLE_SUPER_ADMIN role not found."));

            User adminUser = userRegistrationService.registerNewUser(
                    adminEmail,
                    adminPassword,
                    adminFirstName,
                    adminLastName,
                    adminDisplayName,
                    Set.of(adminRole));

            if (adminUser != null && adminUser.getUserAuth() != null) {
                UserAuth adminAuth = adminUser.getUserAuth();
                adminAuth.setVerified(true);
                adminAuth.setVerificationToken(null);
                userAuthRepository.save(adminAuth);
                System.out.println("Super Admin verification state explicitly forced to ACTIVE.");
            }

            System.out.println("Super Admin '" + adminEmail + "' successfully seeded with Argon2id!");
        }
    }

    private boolean isAnyFieldNullOrBlank() {
        return adminEmail == null || adminEmail.isBlank() ||
                adminPassword == null || adminPassword.isBlank() ||
                adminFirstName == null || adminFirstName.isBlank() ||
                adminLastName == null || adminLastName.isBlank() ||
                adminDisplayName == null || adminDisplayName.isBlank();
    }
}
