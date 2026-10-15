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

    @Transactional(readOnly = true)
    public Recipe getRecipeByPublicId(java.util.UUID publicId) {
        return recipeRepository.findByPublicId(publicId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Recipe record not found for public ID: " + publicId));
    }

    /**
     * Unpacks a saved database Recipe entity tree back into a flat Form DTO
     * structure for form edits
     */
    @Transactional(readOnly = true)
    public RecipeForm prepareFormFromRecipe(Recipe recipe) {
        RecipeForm form = new RecipeForm();
        form.setTitle(recipe.getTitle());
        form.setDescription(recipe.getDescription());
        form.setServings(recipe.getServings());
        form.setRecipeVisibilityId(
                recipe.getRecipeVisibility() != null ? recipe.getRecipeVisibility().getRecipeVisibilityId() : null);

        form.getSteps().clear();

        if (recipe.getSteps() != null) {
            for (RecipeStep step : recipe.getSteps()) {
                StepForm sf = new StepForm();
                sf.setInstructionText(step.getInstructionText());

                sf.getIngredients().clear();
                if (step.getStepIngredients() != null) {
                    for (RecipeStepIngredient si : step.getStepIngredients()) {
                        IngredientForm inf = new IngredientForm();
                        inf.setIngredientId(si.getIngredient() != null ? si.getIngredient().getIngredientId() : null);
                        inf.setAmount(si.getAmount());
                        inf.setMeasurementId(
                                si.getMeasurement() != null ? si.getMeasurement().getMeasurementId() : null);
                        inf.setIngredientNote(si.getIngredientNote());
                        sf.getIngredients().add(inf);
                    }
                }
                form.getSteps().add(sf);
            }
        }
        return form;
    }

    /**
     * Fetches a single recipe by its primary database ID, throwing an exception if
     * not found
     */
    @Transactional(readOnly = true)
    public Recipe getRecipeById(Integer recipeId) {
        // Assuming your repository uses an Integer or Long primary key mapping
        // If your repository uses Long, use: Long.valueOf(recipeId)
        return recipeRepository.findById(recipeId)
                .orElseThrow(() -> new EntityNotFoundException("Recipe not found with ID: " + recipeId));
    }

    @Transactional
    public Recipe saveOrUpdateRecipe(RecipeForm form, User creator) {
        Recipe recipe;

        // 1. Check if we are updating an existing recipe or creating a new one
        if (form.getRecipeId() != null) {
            recipe = recipeRepository.findById(form.getRecipeId())
                    .orElseThrow(() -> new EntityNotFoundException("Recipe not found with ID: " + form.getRecipeId()));

            // Clear out old instruction rows to prevent duplicate step index bugs
            recipe.getSteps().clear();
            recipeRepository.saveAndFlush(recipe); // Force an instant database synchronize flush
        } else {
            recipe = new Recipe();
            recipe.setUser(creator); // Only bind the author profile row on a fresh creation
        }

        // 2. Overwrite or populate general recipe metadata values
        recipe.setTitle(form.getTitle().trim());
        recipe.setDescription(form.getDescription().trim());
        recipe.setServings(form.getServings());

        if (form.getRecipeVisibilityId() != null) {
            recipe.setRecipeVisibility(visibilityRepository.findById(form.getRecipeVisibilityId())
                    .orElseThrow(() -> new EntityNotFoundException("Visibility profile missing")));
        }

        // 3. Rebuild the cascading timeline steps array cleanly
        if (form.getSteps() != null) {
            for (int i = 0; i < form.getSteps().size(); i++) {
                StepForm sf = form.getSteps().get(i);

                RecipeStep step = new RecipeStep();
                step.setRecipe(recipe);
                step.setStepNumber(i + 1);
                step.setInstructionText(sf.getInstructionText().trim());

                if (sf.getIngredients() != null) {
                    for (IngredientForm inf : sf.getIngredients()) {
                        if (inf == null || inf.getIngredientId() == null || inf.getMeasurementId() == null)
                            continue;

                        RecipeStepIngredient si = new RecipeStepIngredient();
                        si.setRecipeStep(step);
                        si.setRecipe(recipe);
                        si.setAmount(inf.getAmount());
                        si.setIngredientNote(inf.getIngredientNote());

                        si.setIngredient(ingredientRepository.findById(inf.getIngredientId())
                                .orElseThrow(() -> new EntityNotFoundException("Ingredient missing")));
                        si.setMeasurement(measurementRepository.findById(inf.getMeasurementId())
                                .orElseThrow(() -> new EntityNotFoundException("Measurement unit missing")));

                        step.getStepIngredients().add(si);
                    }
                }
                recipe.getSteps().add(step);
            }
        }

        return recipeRepository.save(recipe);
    }

}
