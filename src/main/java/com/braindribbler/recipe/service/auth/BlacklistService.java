package com.braindribbler.recipe.service.auth;

import com.braindribbler.recipe.domain.auth.BlacklistItem;
import com.braindribbler.recipe.repository.auth.BlacklistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class BlacklistService {

    private final BlacklistRepository blacklistRepository;

    public BlacklistService(BlacklistRepository blacklistRepository) {
        this.blacklistRepository = blacklistRepository;
    }

    @Transactional(readOnly = true)
    public List<BlacklistItem> getAllItems() {
        return blacklistRepository.findAll();
    }

    @Transactional
    public BlacklistItem addPattern(BlacklistItem item) {
        // 1. Enforce strict uniqueness validation
        if (blacklistRepository.existsByPattern(item.getPattern())) {
            throw new IllegalArgumentException("This pattern is already blacklisted.");
        }

        // 2. Format and clean up raw string entry values
        String cleanPattern = item.getPattern().trim().toLowerCase();
        item.setPattern(cleanPattern);

        // 3. Precise, patched IP protocol auto-detection logic
        if (cleanPattern.contains("/")) {
            item.setType("SUBNET");
        } else if (cleanPattern.matches("^[0-9.]+$") || cleanPattern.contains(":")) {
            item.setType("IP");
        } else {
            item.setType("DOMAIN");
        }

        return blacklistRepository.save(item);
    }

    @Transactional
    public void removePattern(Long id) {
        if (!blacklistRepository.existsById(id)) {
            throw new IllegalArgumentException("Target threat pattern ID not found.");
        }
        blacklistRepository.deleteById(id);
    }

    @Transactional
    public BlacklistItem updatePattern(Long id, BlacklistItem updatedDetails) {
        return blacklistRepository.findById(id)
                .map(existingItem -> {
                    String cleanPattern = updatedDetails.getPattern().trim().toLowerCase();

                    // If they changed the pattern text, verify the new pattern isn't already taken
                    if (!existingItem.getPattern().equalsIgnoreCase(cleanPattern) &&
                            blacklistRepository.existsByPattern(cleanPattern)) {
                        throw new IllegalArgumentException("This pattern is already registered on another entry.");
                    }

                    existingItem.setPattern(cleanPattern);
                    existingItem.setReason(updatedDetails.getReason());

                    // Recalculate type protocol configurations on modifications
                    if (cleanPattern.contains("/")) {
                        existingItem.setType("SUBNET");
                    } else if (cleanPattern.matches("^[0-9.]+$") || cleanPattern.contains(":")) {
                        existingItem.setType("IP");
                    } else {
                        existingItem.setType("DOMAIN");
                    }

                    return blacklistRepository.save(existingItem);
                })
                .orElseThrow(() -> new IllegalArgumentException("Target threat entry ID not found."));
    }
}
