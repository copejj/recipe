package com.braindribbler.recipe.service.auth;

import com.braindribbler.recipe.domain.auth.ProfileVisibility;
import com.braindribbler.recipe.domain.auth.Role;
import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.domain.auth.UserAuth;
import com.braindribbler.recipe.repository.auth.ProfileVisibilityRepository;
import com.braindribbler.recipe.repository.auth.UserAuthRepository;
import com.braindribbler.recipe.repository.auth.UserRepository;
import com.braindribbler.recipe.service.MailgunEmailService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final ProfileVisibilityRepository profileVisibilityRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailgunEmailService mailgunEmailService;

    @Value("${app.api.host}")
    private String appApiHost;

    public UserRegistrationService(UserRepository userRepository,
            UserAuthRepository userAuthRepository,
            ProfileVisibilityRepository profileVisibilityRepository,
            PasswordEncoder passwordEncoder,
            MailgunEmailService mailgunEmailService) {
        this.userRepository = userRepository;
        this.userAuthRepository = userAuthRepository;
        this.profileVisibilityRepository = profileVisibilityRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailgunEmailService = mailgunEmailService;
    }

    @Transactional
    public User registerNewUser(String email, String rawPassword, String firstName, String lastName,
            String displayName, Set<Role> assignedRoles) {

        if (userRepository.existsByUserAuthEmail(email)) {
            throw new IllegalArgumentException("An account with this email address already exists.");
        }

        ProfileVisibility defaultVisibility = profileVisibilityRepository.findByVisibilityName("PUBLIC")
                .orElseThrow(() -> new IllegalStateException("Default profile visibility configuration not found."));

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setDisplayName(displayName);
        user.setProfileVisibility(defaultVisibility);

        if (assignedRoles != null && !assignedRoles.isEmpty()) {
            user.setRoles(assignedRoles);
        }

        UserAuth auth = new UserAuth();
        auth.setEmail(email);
        auth.setPassword(passwordEncoder.encode(rawPassword));
        auth.setUser(user);

        auth.setVerified(false);
        UUID token = UUID.randomUUID();
        auth.setVerificationToken(token);

        user.setUserAuth(auth);

        User savedUser = userRepository.save(user);

        return savedUser;
    }

    public void sendVerificationEmail(User user) {
        UserAuth auth = user.getUserAuth();
        String verificationLink = appApiHost + "/verify?token=" + auth.getVerificationToken();

        String emailSubject = "Verify your Braindribbler Recipe Account";
        String emailBody = "Hi " + user.getFirstName() + ",\n\n" +
                "Thanks for joining Braindribbler Recipes! Please click the link below to verify your account:\n" +
                verificationLink + "\n\n" +
                "Happy Cooking!";

        String email = auth.getEmail();
        try {
            mailgunEmailService.sendEmail(email, emailSubject, emailBody);
        } catch (Exception e) {
            // Log the error but allow the method to complete so the user's browser doesn't
            // throw a 500 error
            System.err.println(
                    "CRITICAL: Failed to dispatch verification email to " + email + ". Error: " + e.getMessage());
        }
    }

    @Transactional
    public void verifyUserToken(UUID token) {
        UserAuth authObj = userAuthRepository.findByVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("This verification link is invalid or has expired."));

        authObj.setVerified(true);
        authObj.setVerificationToken(null); // Wipe the token footprint right away so it can't be clicked again

        userAuthRepository.save(authObj);
    }
}
