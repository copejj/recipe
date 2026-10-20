package com.braindribbler.recipe.service.admin;

import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.repository.auth.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Fetches all registered users for the management dashboard
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Looks up the authenticated user by email and retrieves their calculated rank
     * value
     */
    @Transactional(readOnly = true)
    public int getHighestRoleRankForUserEmail(String email) {
        return userRepository.findByUserAuthEmail(email)
                .map(user -> user != null ? user.getHighestRoleRankValue() : 0)
                .orElse(0);
    }

    /**
     * Checks user existence and safely deletes a user row by public UUID
     */
    @Transactional
    public void deleteUserByPublicId(UUID publicId) {
        if (!userRepository.existsByPublicId(publicId)) {
            throw new IllegalArgumentException("User matching that identifier does not exist.");
        }
        userRepository.deleteByPublicId(publicId);
    }

    /**
     * Locates a system user by their public UUID.
     * Throws an IllegalArgumentException if no match is found.
     */
    @Transactional(readOnly = true)
    public User getUserByPublicId(UUID publicId) {
        if (publicId == null) {
            throw new IllegalArgumentException("The provided public identifier cannot be null.");
        }

        return userRepository.findByPublicId(publicId)
                .orElseThrow(
                        () -> new IllegalArgumentException("No chef registry record matches identifier: " + publicId));
    }
}
