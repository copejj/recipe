package com.braindribbler.recipe.service.recipe;

import com.braindribbler.recipe.domain.recipe.Ingredient;
import com.braindribbler.recipe.domain.recipe.IngredientClassification;
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

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.Collections;
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
    public Ingredient quickRegisterIngredient(String name, Integer groceryCategoryId,
            IngredientClassification classification, User creator) {
        String trimmedName = name.trim();

        // Check against the case-and-whitespace-insensitive partial master index
        Optional<Ingredient> collision = ingredientRepository.findVerifiedCollisionByCanonicalName(trimmedName);
        if (collision.isPresent()) {
            throw new IllegalArgumentException(
                    "A verified master ingredient matching '" + trimmedName
                            + "' already exists.");
        }

        GroceryCategory category = groceryCategoryRepository.findById(groceryCategoryId)
                .orElseThrow(() -> new EntityNotFoundException("Selected Grocery Category Section not found."));

        Ingredient ingredient = new Ingredient();
        ingredient.setIngredientName(trimmedName);
        ingredient.setGroceryCategory(category);
        ingredient.setCreatedByUser(creator);
        ingredient.setIsVerified(false);
        ingredient.setClassification(classification != null ? classification
                : IngredientClassification.SOLID);

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

    @Transactional
    public void approveAndModifyIngredient(Integer ingredientId, String updatedName, Integer updatedCategoryId,
            com.braindribbler.recipe.domain.recipe.IngredientClassification classification) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Ingredient missing"));

        String finalName = (updatedName != null && !updatedName.trim().isEmpty()) ? updatedName.trim()
                : ingredient.getIngredientName();

        // A. Look ahead to make sure another master isn't already verified under this
        // canonical name
        Optional<Ingredient> collision = ingredientRepository.findVerifiedCollisionByCanonicalName(finalName);
        if (collision.isPresent() && !collision.get().getIngredientId().equals(ingredientId)) {
            throw new IllegalArgumentException(
                    "An approved master ingredient matching '" + finalName
                            + "' already exists. Please merge this duplicate instead.");
        }

        // B. Apply modifications
        if (updatedName != null && !updatedName.trim().isEmpty()) {
            ingredient.setIngredientName(finalName);
        }
        if (updatedCategoryId != null) {
            ingredient.setGroceryCategory(groceryCategoryRepository.findById(updatedCategoryId)
                    .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Category missing")));
        }
        if (classification != null) {
            ingredient.setClassification(classification);
        }

        // C. Promote globally
        ingredient.setIsVerified(true);
        ingredientRepository.save(ingredient);

        // ◄ SERVICE CALL 1: Idiomatic bulk update to instantly wake up and unlock all
        // matching duplicate entries
        ingredientRepository.unlockUnverifiedCollisionsByCanonicalName(finalName);
    }

    /**
     * Manually registers a brand new master ingredient, skipping the review queue
     * instantly
     */
    @Transactional
    public Ingredient registerVerifiedIngredient(String name, Integer categoryId,
            com.braindribbler.recipe.domain.recipe.IngredientClassification classification) { // ◄ UPDATED
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Ingredient master name cannot be empty."); //
        }

        Ingredient ingredient = new Ingredient();
        ingredient.setIngredientName(name.trim()); //
        ingredient.setIsVerified(true); // 🔥 Promotes item straight to global cookbook options instantly

        // Connect structural foreign key lookup dependency row
        GroceryCategory category = groceryCategoryRepository.findById(categoryId)
                .orElseThrow(
                        () -> new EntityNotFoundException("Grocery category section not found with ID: " + categoryId)); //
        ingredient.setGroceryCategory(category); //

        // NEW: Set management track classifications parameters
        ingredient.setClassification(classification != null ? classification
                : com.braindribbler.recipe.domain.recipe.IngredientClassification.SOLID);

        return ingredientRepository.save(ingredient); //
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

    @Transactional(readOnly = true)
    public long getFreshIngredientsCount(Integer userId) {
        if (userId == null)
            return 0;

        // Pass the raw integer ID straight into the repository query
        return ingredientRepository.countByCreatedByUserIdAndIsVerifiedFalseAndIsLockedFalse(userId);
    }

    @Transactional(readOnly = true)
    public List<Ingredient> getUnverifiedIngredientsByUser(Integer userId) {
        if (userId == null)
            return Collections.emptyList();

        return ingredientRepository.findByCreatedByUserIdAndIsVerifiedFalseAndIsLockedFalse(userId);
    }

    @Transactional
    public void updateUserSubmittedIngredient(Integer id, String name, Integer categoryId, String classification,
            Integer userId) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ingredient not found ID: " + id));

        // A. Validate WHO is making the request first
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isNotModerator = auth == null || auth.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("can_moderate_ingredients"));

        if (isNotModerator) {
            if (Boolean.TRUE.equals(ingredient.getIsLocked())) {
                throw new SecurityException("This ingredient has been locked and can no longer be edited.");
            }

            if (!ingredient.getCreatedByUserId().equals(userId)) {
                throw new SecurityException("Unauthorized alteration attempt on restricted ingredient entity.");
            }
        }

        // ◄ SERVICE CALL 2: Idiomatic lookahead check to block names matching any
        // verified master asset
        Optional<Ingredient> collision = ingredientRepository.findVerifiedCollisionByCanonicalName(name);
        if (collision.isPresent() && !collision.get().getIngredientId().equals(id)) {
            throw new IllegalArgumentException("A verified master ingredient matching '" + name
                    + "' already exists. Please merge the records instead of renaming.");
        }

        // B. Save changes if all guards pass
        GroceryCategory cat = groceryCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid grocery category selection."));

        ingredient.setIngredientName(name);
        ingredient.setGroceryCategory(cat);
        ingredient.setClassification(IngredientClassification.valueOf(classification));

        ingredientRepository.save(ingredient);
    }

    /**
     * Locks an unverified ingredient, removing it from the admin queue
     * and blocking future editing.
     */
    @Transactional
    public void lockAndHideIngredient(Integer ingredientId) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new EntityNotFoundException("Ingredient not found with ID: " + ingredientId));

        ingredient.setIsLocked(true);
        ingredientRepository.save(ingredient);
    }

    /**
     * Finds a verified master ingredient by checking a case and whitespace
     * insensitive expression.
     */
    @Transactional(readOnly = true)
    public Optional<Ingredient> getVerifiedIngredientByCanonicalName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        return ingredientRepository.findVerifiedCollisionByCanonicalName(name);
    }

}
