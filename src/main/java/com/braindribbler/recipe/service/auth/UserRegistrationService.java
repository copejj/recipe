package com.braindribbler.recipe.service.auth;

import com.braindribbler.recipe.domain.auth.ProfileVisibility;
import com.braindribbler.recipe.domain.auth.Role;
import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.domain.auth.UserAuth;
import com.braindribbler.recipe.repository.auth.ProfileVisibilityRepository;
import com.braindribbler.recipe.repository.auth.RoleRepository;
import com.braindribbler.recipe.repository.auth.UserAuthRepository;
import com.braindribbler.recipe.repository.auth.UserRepository;
import com.braindribbler.recipe.service.MailgunEmailService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final ProfileVisibilityRepository profileVisibilityRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailgunEmailService mailgunEmailService;
    private final RoleRepository roleRepository;

    private static final long VERIFICATION_RESEND_COOLDOWN_MINUTES = 15;
    private static final long VERIFICATION_TOKEN_VALIDITY_HOURS = 24;

    @Value("${app.api.host}")
    private String appApiHost;

    public UserRegistrationService(UserRepository userRepository,
            UserAuthRepository userAuthRepository,
            ProfileVisibilityRepository profileVisibilityRepository,
            PasswordEncoder passwordEncoder,
            MailgunEmailService mailgunEmailService,
            RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.userAuthRepository = userAuthRepository;
        this.profileVisibilityRepository = profileVisibilityRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailgunEmailService = mailgunEmailService;
        this.roleRepository = roleRepository;
    }

    public void sendVerificationEmail(User user) {
        UserAuth auth = user.getUserAuth();
        String verificationLink = appApiHost + "/verify?token=" + auth.getVerificationToken();

        String emailSubject = "Verify your Brain Dribbler Recipe Account";
        String emailBody = "Hi " + user.getFirstName() + ",\n\n" +
                "Thanks for joining Brain Dribbler Recipes! Please click the link below to verify your account:\n" +
                verificationLink + "\n\n" +
                "Happy Cooking!";

        String email = auth.getEmail();
        try {
            mailgunEmailService.sendHtmlEmail(
                    email,
                    emailSubject,
                    "email/verify-email",
                    Map.of(
                            "firstName", user.getFirstName(),
                            "actionUrl", verificationLink
                    ),
                    emailBody
            );
        } catch (Exception e) {
            System.err.println(
                    "CRITICAL: Failed to dispatch verification email to " + email + ". Error: " + e.getMessage());
        }
    }

    @Transactional
    public Optional<User> registerNewUser(String email, String rawPassword, String firstName, String lastName,
            String displayName, Set<Role> assignedRoles) {

        Optional<UserAuth> existingAuthOpt = userAuthRepository.findByEmail(email);

        if (existingAuthOpt.isPresent()) {
            UserAuth existingAuth = existingAuthOpt.get();

            // 1. Already verified -> Silently ignore to prevent account enumeration
            if (existingAuth.isVerified()) {
                return Optional.empty();
            }

            // 2. Unverified, but within cooldown window -> Silently hold off
            if (existingAuth.getVerificationSentAt() != null &&
                existingAuth.getVerificationSentAt().isAfter(
                        OffsetDateTime.now().minusMinutes(VERIFICATION_RESEND_COOLDOWN_MINUTES))) {
                return Optional.empty();
            }

            // 3. Unverified and past cooldown -> Issue fresh token & resend
            existingAuth.setVerificationToken(UUID.randomUUID());
            existingAuth.setVerificationSentAt(OffsetDateTime.now());
            existingAuth.setUpdatedAt(OffsetDateTime.now());
            userAuthRepository.save(existingAuth);

            return Optional.of(existingAuth.getUser());
        }

        // 4. New user registration
        ProfileVisibility defaultVisibility = profileVisibilityRepository.findByVisibilityName("PUBLIC")
                .orElseThrow(() -> new IllegalStateException("Default profile visibility configuration not found."));

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setDisplayName(displayName);
        user.setProfileVisibility(defaultVisibility);

        if (assignedRoles != null && !assignedRoles.isEmpty()) {
            user.setRoles(assignedRoles);
        } else {
            Role defaultRole = roleRepository.findByRoleName("ROLE_USER")
                    .orElseThrow(() -> new IllegalStateException(
                            "Required database role configuration 'ROLE_USER' not found."));
            user.addRole(defaultRole);
        }

        UserAuth auth = new UserAuth();
        auth.setEmail(email);
        auth.setPassword(passwordEncoder.encode(rawPassword));
        auth.setUser(user);
        auth.setVerified(false);
        auth.setVerificationToken(UUID.randomUUID());
        auth.setVerificationSentAt(OffsetDateTime.now());
        user.setUserAuth(auth);

        return Optional.of(userRepository.save(user));
    }

    @Transactional
    public void verifyUserToken(UUID token) {
        UserAuth authObj = userAuthRepository.findByVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("This verification link is invalid or has expired."));

        if (authObj.getVerificationSentAt() != null &&
            authObj.getVerificationSentAt().isBefore(
                    OffsetDateTime.now().minusHours(VERIFICATION_TOKEN_VALIDITY_HOURS))) {
            throw new IllegalArgumentException("This verification link has expired. Please register again to receive a new link.");
        }

        authObj.setVerified(true);
        authObj.setVerificationToken(null);
        authObj.setVerificationSentAt(null);
        authObj.setUpdatedAt(OffsetDateTime.now());

        userAuthRepository.save(authObj);
    }
}
