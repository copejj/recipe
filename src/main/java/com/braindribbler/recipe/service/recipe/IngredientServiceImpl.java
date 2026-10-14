package com.braindribbler.recipe.service.recipe;

import com.braindribbler.recipe.dto.recipe.PendingIngredientProjection;
import com.braindribbler.recipe.domain.recipe.Ingredient;
import com.braindribbler.recipe.repository.recipe.IngredientRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IngredientServiceImpl implements IngredientService {

    private final IngredientRepository ingredientRepository;

    public IngredientServiceImpl(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingIngredientProjection> getUnverifiedIngredients() {
        return ingredientRepository.findUnverifiedIngredients();
    }

    @Override
    @Transactional
    public void approveIngredient(Integer ingredientId) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new EntityNotFoundException("Ingredient not found with ID: " + ingredientId));

        ingredient.setIsVerified(true);
        ingredientRepository.save(ingredient);
    }

    @Override
    @Transactional
    public void mergeAndRejectIngredient(Integer duplicateIngredientId, Integer targetIngredientId) {
        Ingredient duplicate = ingredientRepository.findById(duplicateIngredientId)
                .orElseThrow(() -> new EntityNotFoundException("Duplicate ingredient source not found"));

        Ingredient target = ingredientRepository.findById(targetIngredientId)
                .orElseThrow(() -> new EntityNotFoundException("Target canonical ingredient not found"));

        // TODO: Update recipe ingredients referencing 'duplicateIngredientId' to point
        // to 'targetIngredientId'
        // right here before dropping the duplicate to protect data integration!

        ingredientRepository.delete(duplicate);
    }
}
