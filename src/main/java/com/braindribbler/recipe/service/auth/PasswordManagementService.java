package com.braindribbler.recipe.service.auth;

import com.braindribbler.recipe.domain.auth.UserAuth;
import com.braindribbler.recipe.repository.auth.UserAuthRepository;
import com.braindribbler.recipe.service.MailgunEmailService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordManagementService {

    private final UserAuthRepository userAuthRepository;
    private final MailgunEmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public PasswordManagementService(UserAuthRepository userAuthRepository,
            MailgunEmailService emailService,
            PasswordEncoder passwordEncoder) {
        this.userAuthRepository = userAuthRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public boolean initiatePasswordReset(String email, String baseUrl) {
        Optional<UserAuth> authOpt = userAuthRepository.findByEmail(email);
        if (authOpt.isEmpty()) {
            return false; // Don't leak details, handle cleanly in controller
        }

        UserAuth auth = authOpt.get();
        auth.setResetPasswordToken(UUID.randomUUID());
        auth.setResetPasswordExpiresAt(OffsetDateTime.now().plusHours(2)); // Expires in 2 hours
        auth.setUpdatedAt(OffsetDateTime.now());
        userAuthRepository.save(auth);

        String resetLink = baseUrl + "/password/reset?token=" + auth.getResetPasswordToken();
        String firstName = (auth.getUser() != null && auth.getUser().getFirstName() != null)
                ? auth.getUser().getFirstName()
                : "Chef";

        String emailText = "Hi " + firstName + ",\n\n" +
                "We received a request to reset your Brain Dribbler Recipes password. Click the link below to choose a new one:\n\n" +
                resetLink + "\n\n" +
                "This link will expire in 2 hours. If you didn't request a password reset, you can safely ignore this email.\n\n" +
                "Happy Cooking!";

        try {
            emailService.sendHtmlEmail(
                    auth.getEmail(),
                    "Password Reset Request",
                    "email/reset-password",
                    Map.of(
                            "firstName", firstName,
                            "actionUrl", resetLink
                    ),
                    emailText
            );
        } catch (Exception e) {
            System.err.println(
                    "CRITICAL: Failed to dispatch password reset email to " + email + ". Error: " + e.getMessage());
        }

        return true;
    }

    @Transactional
    public boolean completePasswordReset(UUID token, String newPassword) {
        Optional<UserAuth> authOpt = userAuthRepository.findByResetPasswordToken(token);
        if (authOpt.isEmpty()) {
            return false;
        }

        UserAuth auth = authOpt.get();
        if (auth.getResetPasswordExpiresAt().isBefore(OffsetDateTime.now())) {
            return false; // Token expired
        }

        // Apply new password updates
        auth.setPassword(passwordEncoder.encode(newPassword));
        auth.setPasswordChangedAt(OffsetDateTime.now());
        auth.setResetPasswordToken(null); // Obliterate token after single use
        auth.setResetPasswordExpiresAt(null);
        auth.setUpdatedAt(OffsetDateTime.now());

        userAuthRepository.save(auth);
        return true;
    }
}
