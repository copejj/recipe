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

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    private final GroceryCategoryRepository groceryCategoryRepository;

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
            return ingredientRepository.findAvailableIngredientsForUser(null);
        }
        return ingredientRepository.findAvailableIngredientsForUser(user);
    }
    @Transactional(readOnly = true)
    public Optional<UserMessage> getLatestActiveMessageForUser(Integer userId) {
        return userMessageRepository.findLatestActiveMessage(userId, OffsetDateTime.now());
    }

    @Cacheable(value = "groceryCategories")
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

        Optional<Ingredient> collision = ingredientRepository.findVerifiedCollisionByCanonicalName(trimmedName);
        if (collision.isPresent()) {
            throw new IllegalArgumentException(
                    "A verified master ingredient matching '" + trimmedName + "' already exists.");
        }

        GroceryCategory category = groceryCategoryRepository.findById(groceryCategoryId)
                .orElseThrow(() -> new EntityNotFoundException("Selected Grocery Category Section not found."));

        Ingredient ingredient = new Ingredient();
        ingredient.setIngredientName(trimmedName);
        ingredient.setGroceryCategory(category);
        ingredient.setCreatedByUser(creator);
        ingredient.setIsVerified(false);
        ingredient.setClassification(classification != null ? classification : IngredientClassification.SOLID);

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
    @CacheEvict(value = "groceryCategories", allEntries = true)
    @Transactional
    public void approveAndModifyIngredient(Integer ingredientId, String updatedName, Integer updatedCategoryId,
            com.braindribbler.recipe.domain.recipe.IngredientClassification classification) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Ingredient missing"));

        String finalName = (updatedName != null && !updatedName.trim().isEmpty()) ? updatedName.trim()
                : ingredient.getIngredientName();

        Optional<Ingredient> collision = ingredientRepository.findVerifiedCollisionByCanonicalName(finalName);
        if (collision.isPresent() && !collision.get().getIngredientId().equals(ingredientId)) {
            throw new IllegalArgumentException(
                    "An approved master ingredient matching '" + finalName + "' already exists. Please merge this duplicate instead.");
        }

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

        ingredient.setIsVerified(true);
        ingredientRepository.save(ingredient);

        ingredientRepository.unlockUnverifiedCollisionsByCanonicalName(finalName);
    }

    @Transactional
    public Ingredient registerVerifiedIngredient(String name, Integer categoryId,
            com.braindribbler.recipe.domain.recipe.IngredientClassification classification) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Ingredient master name cannot be empty.");
        }

        Ingredient ingredient = new Ingredient();
        ingredient.setIngredientName(name.trim());
        ingredient.setIsVerified(true);

        GroceryCategory category = groceryCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Grocery category section not found with ID: " + categoryId));
        ingredient.setGroceryCategory(category);

        ingredient.setClassification(classification != null ? classification : com.braindribbler.recipe.domain.recipe.IngredientClassification.SOLID);

        return ingredientRepository.save(ingredient);
    }

    @Transactional(readOnly = true)
    public Ingredient getIngredientById(Integer id) {
        return ingredientRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Ingredient not found with ID: " + id));
    }
    @Transactional
    public void deleteIngredientById(Integer ingredientId) {
        if (!ingredientRepository.existsById(ingredientId)) {
            throw new EntityNotFoundException("Ingredient not found with ID: " + ingredientId);
        }

        try {
            ingredientRepository.deleteById(ingredientId);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException(
                    "Cannot delete this ingredient. It is currently being used in active recipe steps!");
        }
    }

    @Cacheable(value = "groceryCategories")
    @Transactional(readOnly = true)
    public List<GroceryCategory> getAllGroceryCategoriesSortedByOrder() {
        return groceryCategoryRepository.findAllByOrderByDisplayOrderAsc();
    }

    @Transactional
    public void retractIngredientFromGlobalPool(Integer ingredientId) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Ingredient not found with ID: " + ingredientId));

        ingredient.setIsVerified(false);
        ingredientRepository.save(ingredient);
    }

    @Transactional(readOnly = true)
    public long getFreshIngredientsCount(Integer userId) {
        if (userId == null)
            return 0;

        return ingredientRepository.countByCreatedByUserIdAndIsVerifiedFalseAndIsLockedFalse(userId);
    }

    @Transactional(readOnly = true)
    public List<Ingredient> getUnverifiedIngredientsByUser(Integer userId) {
        if (userId == null)
            return Collections.emptyList();

        return ingredientRepository.findByCreatedByUserIdAndIsVerifiedFalseAndIsLockedFalse(userId);
    }

    @CacheEvict(value = "groceryCategories", allEntries = true)
    @Transactional
    public void updateUserSubmittedIngredient(Integer id, String name, Integer categoryId, String classification,
            Integer userId) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ingredient not found ID: " + id));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isNotModerator = auth == null || auth.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("can_moderate_recipe_metadata"));

        if (isNotModerator) {
            if (Boolean.TRUE.equals(ingredient.getIsLocked())) {
                throw new SecurityException("This ingredient has been locked and can no longer be edited.");
            }

            if (!ingredient.getCreatedByUserId().equals(userId)) {
                throw new SecurityException("Unauthorized alteration attempt on restricted ingredient entity.");
            }
        }

        Optional<Ingredient> collision = ingredientRepository.findVerifiedCollisionByCanonicalName(name);
        if (collision.isPresent() && !collision.get().getIngredientId().equals(id)) {
            throw new IllegalArgumentException("A verified master ingredient matching '" + name
                    + "' already exists. Please merge the records instead of renaming.");
        }

        GroceryCategory cat = groceryCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid grocery category selection."));

        ingredient.setIngredientName(name);
        ingredient.setGroceryCategory(cat);
        ingredient.setClassification(IngredientClassification.valueOf(classification));

        ingredientRepository.save(ingredient);
    }

    @Transactional
    public void lockAndHideIngredient(Integer ingredientId) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new EntityNotFoundException("Ingredient not found with ID: " + ingredientId));

        ingredient.setIsLocked(true);
        ingredientRepository.save(ingredient);
    }

    @Transactional(readOnly = true)
    public Optional<Ingredient> getVerifiedIngredientByCanonicalName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        return ingredientRepository.findVerifiedCollisionByCanonicalName(name);
    }
}
