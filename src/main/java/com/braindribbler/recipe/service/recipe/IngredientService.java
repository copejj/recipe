package com.braindribbler.recipe.service.recipe;

import com.braindribbler.recipe.domain.auth.UserMessage;
import com.braindribbler.recipe.domain.recipe.Ingredient;
import com.braindribbler.recipe.dto.recipe.PendingIngredientProjection;
import com.braindribbler.recipe.repository.auth.UserMessageRepository;
import com.braindribbler.recipe.repository.recipe.IngredientRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final UserMessageRepository userMessageRepository;

    public IngredientService(IngredientRepository ingredientRepository, UserMessageRepository userMessageRepository) {
        this.ingredientRepository = ingredientRepository;
        this.userMessageRepository = userMessageRepository;
    }

    /**
     * Fetches all unverified ingredients using the custom repository projection
     */
    @Transactional(readOnly = true)
    public List<PendingIngredientProjection> getUnverifiedIngredients() {
        return ingredientRepository.findUnverifiedIngredients();
    }

    /**
     * Approves and verifies a user-submitted ingredient by setting is_verified to
     * true
     */
    @Transactional
    public void approveIngredient(Integer ingredientId) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new EntityNotFoundException("Ingredient not found with ID: " + ingredientId));

        ingredient.setIsVerified(true);
        ingredientRepository.save(ingredient);
    }

    /**
     * Merges a duplicate ingredient into a valid target ingredient.
     * All existing recipe steps referencing the duplicate are migrated to the
     * target,
     * and the duplicate ingredient is permanently removed.
     */
    @Transactional
    public void mergeAndRejectIngredient(Integer duplicateId, Integer targetId) {
        if (duplicateId.equals(targetId)) {
            throw new IllegalArgumentException("Cannot merge an ingredient into itself.");
        }

        // Verify both ingredients exist before running modifications
        Ingredient duplicate = ingredientRepository.findById(duplicateId)
                .orElseThrow(
                        () -> new EntityNotFoundException("Duplicate ingredient not found with ID: " + duplicateId));

        if (!ingredientRepository.existsById(targetId)) {
            throw new EntityNotFoundException("Target validation ingredient not found with ID: " + targetId);
        }

        // 1. Update the recipe join table rows pointing to the duplicate ingredient
        ingredientRepository.relinkMergedIngredients(duplicateId, targetId);

        // 2. Safely delete the unverified duplicate row from the database
        ingredientRepository.delete(duplicate);
    }

    @Transactional(readOnly = true)
    public List<Ingredient> getAllIngredientsSorted() {
        // Uses your custom sorted repository query hook
        return ingredientRepository.findAllByOrderByIngredientNameAsc();
    }

    @Transactional(readOnly = true)
    public Optional<UserMessage> getLatestActiveMessageForUser(Integer userId) {
        // Normalizes time explicitly to the database's OffsetDateTime context
        return userMessageRepository.findLatestActiveMessage(userId, java.time.OffsetDateTime.now());
    }

    @Transactional
    public Ingredient saveRawIngredient(Ingredient ing) {
        return ingredientRepository.save(ing);
    }

}
