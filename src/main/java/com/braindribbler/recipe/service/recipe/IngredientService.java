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
    public List<Ingredient> getAllIngredientsSorted() {
        return ingredientRepository.findAllByOrderByIngredientNameAsc();
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
}
