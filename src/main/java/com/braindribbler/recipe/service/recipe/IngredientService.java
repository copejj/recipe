package com.braindribbler.recipe.service.recipe;

import com.braindribbler.recipe.domain.recipe.Ingredient;
import com.braindribbler.recipe.dto.recipe.PendingIngredientProjection;
import com.braindribbler.recipe.repository.recipe.IngredientRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IngredientService {

    private final IngredientRepository ingredientRepository;

    public IngredientService(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
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
}
