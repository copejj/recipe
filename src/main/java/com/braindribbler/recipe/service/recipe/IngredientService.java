package com.braindribbler.recipe.service.recipe;

import com.braindribbler.recipe.domain.recipe.Ingredient;
import com.braindribbler.recipe.domain.recipe.GroceryCategory;
import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.domain.auth.UserMessage;
import com.braindribbler.recipe.dto.recipe.PendingIngredientProjection;
import com.braindribbler.recipe.repository.recipe.IngredientRepository;
import com.braindribbler.recipe.repository.recipe.GroceryCategoryRepository;
import com.braindribbler.recipe.repository.auth.UserMessageRepository;
import jakarta.persistence.EntityNotFoundException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final UserMessageRepository userMessageRepository;
    private final GroceryCategoryRepository groceryCategoryRepository; // ◄ NEW

    public IngredientService(IngredientRepository ingredientRepository,
            UserMessageRepository userMessageRepository,
            GroceryCategoryRepository groceryCategoryRepository) {
        this.ingredientRepository = ingredientRepository;
        this.userMessageRepository = userMessageRepository;
        this.groceryCategoryRepository = groceryCategoryRepository;
    }

    @Transactional(readOnly = true)
    public List<PendingIngredientProjection> getUnverifiedIngredients() {
        return ingredientRepository.findUnverifiedIngredients();
    }

    @Transactional(readOnly = true)
    public List<Ingredient> getAllIngredientsSorted(User user) {
        if (user == null) {
            // Fallback for safety/anonymous access: only show verified items
            return ingredientRepository.findAvailableIngredientsForUser(null);
        }
        return ingredientRepository.findAvailableIngredientsForUser(user);
    }

    @Transactional(readOnly = true)
    public Optional<UserMessage> getLatestActiveMessageForUser(Integer userId) {
        return userMessageRepository.findLatestActiveMessage(userId, OffsetDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<GroceryCategory> getAllGroceryCategories() {
        return groceryCategoryRepository.findAll();
    }

    @Transactional
    public void approveIngredient(Integer ingredientId) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new EntityNotFoundException("Ingredient not found: " + ingredientId));
        ingredient.setIsVerified(true);
        ingredientRepository.save(ingredient);
    }

    @Transactional
    public Ingredient quickRegisterIngredient(String name, Integer groceryCategoryId, User creator) {
        String trimmedName = name.trim();

        if (ingredientRepository.findByIngredientNameIgnoreCase(trimmedName).isPresent()) {
            throw new IllegalArgumentException(
                    "An ingredient with the name '" + trimmedName + "' already exists in the pool.");
        }

        GroceryCategory category = groceryCategoryRepository.findById(groceryCategoryId)
                .orElseThrow(() -> new EntityNotFoundException("Selected Grocery Category Section not found."));

        Ingredient ingredient = new Ingredient();
        ingredient.setIngredientName(trimmedName);
        ingredient.setGroceryCategory(category);
        ingredient.setCreatedByUser(creator);
        ingredient.setIsVerified(false); // Flags it for the admin moderation pipeline panel

        return ingredientRepository.save(ingredient);
    }

    @Transactional
    public void mergeAndRejectIngredient(Integer duplicateId, Integer targetId) {
        if (duplicateId.equals(targetId)) {
            throw new IllegalArgumentException("Cannot merge an ingredient into itself.");
        }
        Ingredient duplicate = ingredientRepository.findById(duplicateId)
                .orElseThrow(() -> new EntityNotFoundException("Duplicate missing: " + duplicateId));

        if (!ingredientRepository.existsById(targetId)) {
            throw new EntityNotFoundException("Target missing: " + targetId);
        }

        ingredientRepository.relinkMergedIngredients(duplicateId, targetId);
        ingredientRepository.delete(duplicate);
    }

    // Inside com.braindribbler.recipe.service.recipe.IngredientService
    @Transactional
    public void approveAndModifyIngredient(Integer ingredientId, String updatedName, Integer updatedCategoryId) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Ingredient missing"));

        // 1. Overwrite the name text if the admin corrected a typo
        if (updatedName != null && !updatedName.trim().isEmpty()) {
            ingredient.setIngredientName(updatedName.trim());
        }

        // 2. Overwrite the grocery section aisle categorization layout mapping if
        // modified
        if (updatedCategoryId != null) {
            ingredient.setGroceryCategory(groceryCategoryRepository.findById(updatedCategoryId)
                    .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Category missing")));
        }

        // 3. Promote globally
        ingredient.setIsVerified(true);
        ingredientRepository.save(ingredient);
    }

    /**
     * Fetches a single ingredient entity by its primary key ID
     */
    @Transactional(readOnly = true)
    public Ingredient getIngredientById(Integer id) {
        return ingredientRepository.findById(id)
                .orElseThrow(
                        () -> new jakarta.persistence.EntityNotFoundException("Ingredient not found with ID: " + id));
    }

    /**
     * Hard-deletes an ingredient from the database pool if it is not actively
     * linked to a recipe step
     */
    @Transactional
    public void deleteIngredientById(Integer ingredientId) {
        // 1. Verify existence before pulling the trigger
        if (!ingredientRepository.existsById(ingredientId)) {
            throw new EntityNotFoundException("Ingredient not found with ID: " + ingredientId);
        }

        try {
            // 2. Attempt a clean removal from the master catalog rows
            ingredientRepository.deleteById(ingredientId);
        } catch (DataIntegrityViolationException e) {
            // 3. Intercept active foreign key usage blocks cleanly to prevent a server
            // crash
            throw new IllegalStateException(
                    "Cannot delete this ingredient. It is currently being used in active recipe steps!");
        }
    }

    /**
     * Fetches all grocery categories ordered by their physical aisle display
     * priority weight
     */
    @Transactional(readOnly = true)
    public List<GroceryCategory> getAllGroceryCategoriesSortedByOrder() {
        // Bridges cleanly to your backing repository query sequence mapping hook
        return groceryCategoryRepository.findAllByOrderByDisplayOrderAsc();
    }

    /**
     * Manually registers a brand new master ingredient, skipping the review queue
     * instantly
     */
    @Transactional
    public Ingredient registerVerifiedIngredient(String name, Integer categoryId) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Ingredient master name cannot be empty.");
        }

        Ingredient ingredient = new Ingredient();
        ingredient.setIngredientName(name.trim());
        ingredient.setIsVerified(true); // 🔥 Promotes item straight to global cookbook options instantly

        // Connect structural foreign key lookup dependency row
        GroceryCategory category = groceryCategoryRepository.findById(categoryId)
                .orElseThrow(
                        () -> new EntityNotFoundException("Grocery category section not found with ID: " + categoryId));
        ingredient.setGroceryCategory(category);

        return ingredientRepository.save(ingredient);
    }

    /**
     * Retracts an ingredient from global pools and drops it back into the
     * moderation queue
     * without breaking database relationships.
     */
    @Transactional
    public void retractIngredientFromGlobalPool(Integer ingredientId) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Ingredient not found with ID: " + ingredientId));

        // Demote tracking flag parameter
        ingredient.setIsVerified(false);
        ingredientRepository.save(ingredient);
    }
}
