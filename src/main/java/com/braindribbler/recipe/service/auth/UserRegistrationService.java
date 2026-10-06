package com.braindribbler.recipe.service.auth;

import com.braindribbler.recipe.domain.auth.ProfileVisibility;
import com.braindribbler.recipe.domain.auth.Role;
import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.domain.auth.UserAuth;
import com.braindribbler.recipe.repository.auth.ProfileVisibilityRepository;
import com.braindribbler.recipe.repository.auth.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final ProfileVisibilityRepository profileVisibilityRepository;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationService(UserRepository userRepository,
            ProfileVisibilityRepository profileVisibilityRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.profileVisibilityRepository = profileVisibilityRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerNewUser(String email, String rawPassword, String firstName, String lastName,
            String displayName, Set<Role> assignedRoles) {

        if (userRepository.existsByUserAuthEmail(email)) {
            throw new IllegalArgumentException("An account with this email address already exists.");
        }

        ProfileVisibility defaultVisibility = profileVisibilityRepository.findByVisibilityName("PUBLIC")
                .orElseThrow(() -> new IllegalStateException("Default profile visibility configuration not found."));

        // Inside your registerNewUser method in UserRegistrationService.java:
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setDisplayName(displayName);
        user.setProfileVisibility(defaultVisibility);

        // Attach the collection of roles directly to the User entity instead of auth
        if (assignedRoles != null && !assignedRoles.isEmpty()) {
            user.setRoles(assignedRoles);
        }

        UserAuth auth = new UserAuth();
        auth.setEmail(email);
        auth.setPassword(passwordEncoder.encode(rawPassword));
        auth.setUser(user);

        user.setUserAuth(auth);

        return userRepository.save(user);
    }
}
