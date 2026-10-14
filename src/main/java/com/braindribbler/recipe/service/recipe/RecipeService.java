package com.braindribbler.recipe.service.recipe;

import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.domain.recipe.*;
import com.braindribbler.recipe.dto.recipe.*;
import com.braindribbler.recipe.repository.recipe.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final MeasurementRepository measurementRepository;
    private final RecipeVisibilityRepository visibilityRepository;

    // Direct Constructor Injection for all domain repositories
    public RecipeService(RecipeRepository recipeRepository,
            IngredientRepository ingredientRepository,
            MeasurementRepository measurementRepository,
            RecipeVisibilityRepository visibilityRepository) {
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.measurementRepository = measurementRepository;
        this.visibilityRepository = visibilityRepository;
    }

    @Transactional(readOnly = true)
    public long getRecipeCountByUserId(Integer userId) {
        // Calls the existing query hook from your repository layer
        return recipeRepository.findByUserUserId(Long.valueOf(userId)).size();
    }

    @Transactional(readOnly = true)
    public long getTotalRecipesCount() {
        return recipeRepository.count();
    }

    @Transactional(readOnly = true)
    public List<Measurement> getAllMeasurements() {
        return measurementRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<RecipeVisibility> getAllVisibilities() {
        return visibilityRepository.findAll();
    }

    @Transactional
    public Recipe saveRecipeFromForm(RecipeForm form, User currentUser) {
        Recipe recipe = new Recipe();
        recipe.setTitle(form.getTitle());
        recipe.setDescription(form.getDescription());
        recipe.setServings(form.getServings());
        recipe.setRecipeNote(form.getRecipeNote());
        recipe.setUser(currentUser);

        RecipeVisibility visibility = visibilityRepository.findById(form.getRecipeVisibilityId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Visibility not found with ID: " + form.getRecipeVisibilityId()));
        recipe.setRecipeVisibility(visibility);

        List<RecipeStep> targetSteps = new ArrayList<>();
        for (int i = 0; i < form.getSteps().size(); i++) {
            StepForm stepForm = form.getSteps().get(i);

            RecipeStep step = new RecipeStep();
            step.setRecipe(recipe);
            step.setStepNumber(i + 1);
            step.setInstructionText(stepForm.getInstructionText());

            List<RecipeStepIngredient> stepIngredients = new ArrayList<>();
            for (IngredientForm ingForm : stepForm.getIngredients()) {
                if (ingForm.getIngredientId() == null || ingForm.getMeasurementId() == null) {
                    continue;
                }

                RecipeStepIngredient link = new RecipeStepIngredient();
                link.setRecipe(recipe);
                link.setRecipeStep(step);
                link.setAmount(ingForm.getAmount());
                link.setIngredientNote(ingForm.getIngredientNote());

                Ingredient ingredient = ingredientRepository.findById(ingForm.getIngredientId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Ingredient missing with ID: " + ingForm.getIngredientId()));
                link.setIngredient(ingredient);

                Measurement measurement = measurementRepository.findById(ingForm.getMeasurementId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Measurement missing with ID: " + ingForm.getMeasurementId()));
                link.setMeasurement(measurement);

                stepIngredients.add(link);
            }
            step.setStepIngredients(stepIngredients);
            targetSteps.add(step);
        }
        recipe.setSteps(targetSteps);

        return recipeRepository.save(recipe);
    }
}
