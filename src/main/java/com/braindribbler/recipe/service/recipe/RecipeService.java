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

    /**
     * Maps the complex nested Form DTO data into database entities and commits them
     */
    @Transactional
    public Recipe saveRecipeFromForm(RecipeForm form, User currentUser) {
        Recipe recipe = new Recipe();
        recipe.setTitle(form.getTitle());
        recipe.setDescription(form.getDescription());
        recipe.setServings(form.getServings());
        recipe.setRecipeNote(form.getRecipeNote());
        recipe.setUser(currentUser); // Link recipe to the logged-in user profile

        // 1. Resolve and wire Global Visibility setting
        RecipeVisibility visibility = visibilityRepository.findById(form.getRecipeVisibilityId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Recipe visibility mapping not found for ID: " + form.getRecipeVisibilityId()));
        recipe.setRecipeVisibility(visibility);

        // 2. Loop through dynamic instruction rows sequentially to assign step
        // positions
        List<RecipeStep> targetSteps = new ArrayList<>();
        for (int i = 0; i < form.getSteps().size(); i++) {
            StepForm stepForm = form.getSteps().get(i);

            RecipeStep step = new RecipeStep();
            step.setRecipe(recipe); // Crucial bi-directional mapping link back to parent
            step.setStepNumber(i + 1); // Normalize auto-assigned step order number
            step.setInstructionText(stepForm.getInstructionText());

            // 3. Process the distinct subset of ingredients scoped exclusively to this
            // single step
            List<RecipeStepIngredient> stepIngredients = new ArrayList<>();
            for (IngredientForm ingForm : stepForm.getIngredients()) {
                // Drop empty dropdown modifications safely
                if (ingForm.getIngredientId() == null || ingForm.getMeasurementId() == null) {
                    continue;
                }

                RecipeStepIngredient link = new RecipeStepIngredient();
                link.setRecipe(recipe); // Ties into global recipe context lookup
                link.setRecipeStep(step); // Ties into step position index lookup
                link.setAmount(ingForm.getAmount());
                link.setIngredientNote(ingForm.getIngredientNote());

                Ingredient ingredient = ingredientRepository.findById(ingForm.getIngredientId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Master ingredient database entry missing: " + ingForm.getIngredientId()));
                link.setIngredient(ingredient);

                Measurement measurement = measurementRepository.findById(ingForm.getMeasurementId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Measurement configuration unit missing: " + ingForm.getMeasurementId()));
                link.setMeasurement(measurement);

                stepIngredients.add(link);
            }
            step.setStepIngredients(stepIngredients);
            targetSteps.add(step);
        }
        recipe.setSteps(targetSteps);

        // Due to cascade = CascadeType.ALL configurations on your models, saving the
        // top-level
        // recipe automatically persists the linked children step rows and composite
        // ingredient links seamlessly!
        return recipeRepository.save(recipe);
    }

    @Transactional(readOnly = true)
    public List<Recipe> getRecipesByUserId(Integer userId) {
        // Calls your existing repository method hook cleanly
        return recipeRepository.findByUserUserId(Long.valueOf(userId));
    }

}
