package com.braindribbler.recipe.service.recipe;

import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.domain.recipe.*;
import com.braindribbler.recipe.dto.recipe.*;
import com.braindribbler.recipe.repository.recipe.*;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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
    public Recipe getRecipeByPublicId(UUID publicId) {
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
        form.setRecipeNote(recipe.getRecipeNote());
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

                        if (si.getIngredient() != null) {
                            inf.setVerified(si.getIngredient().getIsVerified());
                            inf.setCreatedByUserId(si.getIngredient().getCreatedByUserId());
                        }

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

        if (form.getRecipeNote() != null) {
            recipe.setRecipeNote(form.getRecipeNote().trim());
        } else {
            recipe.setRecipeNote(null);
        }

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
                String text = sf.getInstructionText();
                step.setInstructionText(text != null ? text.trim() : "");

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

    /**
     * Safely deletes a recipe by its primary ID. Natively triggers cascading drops
     * for steps and ingredients via database constraints.
     */
    @Transactional
    public void deleteRecipeById(Integer recipeId) {
        // 1. Verify the recipe actually exists before executing delete operations
        if (!recipeRepository.existsById(recipeId)) {
            throw new EntityNotFoundException("Recipe not found with ID: " + recipeId);
        }

        // 2. Fire the deletion command.
        // PostgreSQL catches this and safely sets parent_recipe_id to NULL for
        // forks, while cleanly dropping attached steps and step-ingredients!
        recipeRepository.deleteById(recipeId);
    }

    /**
     * Natively fetches all ingredients sorted alphabetically for clean dropdown
     * select components.
     */
    public List<Ingredient> getAllIngredientsAlphabetical() {
        return ingredientRepository.findAllByOrderByIngredientNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Recipe> searchByIngredientsOnHand(List<Integer> ingredientIds, User loggedInUser) {
        if (ingredientIds == null || ingredientIds.isEmpty()) {
            return Collections.emptyList();
        }

        boolean isLoggedIn = (loggedInUser != null);
        Integer currentUserId = isLoggedIn ? loggedInUser.getUserId() : null;

        // References your ingredient density lookup method
        return recipeRepository.searchRecipesByIngredientsList(ingredientIds, isLoggedIn, currentUserId);
    }

    /**
     * Fetches all recipes a user is allowed to see for general browsing.
     * Updated to invoke the new parameter-based visibility filter query.
     */
    @Transactional(readOnly = true)
    public List<Recipe> getGeneralBrowsingPool(User loggedInUser) {

        boolean isLoggedIn = (loggedInUser != null);
        Integer currentUserId = isLoggedIn ? loggedInUser.getUserId() : null;

        // FIXED: Pointing directly to your clean visibility query method
        return recipeRepository.findAllPublicOrOwned(isLoggedIn, currentUserId);
    }

    /**
     * Aggregates all ingredients across all recipe steps, grouping them
     * to prevent duplicate ingredient display lines in the master checklist.
     */
    @Transactional(readOnly = true)
    public List<RecipeStepIngredient> getAggregatedIngredients(Recipe recipe) {
        if (recipe == null || recipe.getSteps() == null) {
            return java.util.Collections.emptyList();
        }

        // Use a linked hash map to maintain the order in which ingredients appear
        java.util.Map<Integer, RecipeStepIngredient> summaryMap = new java.util.LinkedHashMap<>();

        for (RecipeStep step : recipe.getSteps()) {
            if (step.getStepIngredients() != null) {
                for (RecipeStepIngredient si : step.getStepIngredients()) {
                    Integer ingredientId = si.getIngredient().getIngredientId();

                    if (summaryMap.containsKey(ingredientId)) {
                        // If the ingredient is already in the list, combine the amounts
                        RecipeStepIngredient existing = summaryMap.get(ingredientId);
                        if (existing.getAmount() != null && si.getAmount() != null) {
                            existing.setAmount(existing.getAmount().add(si.getAmount()));
                        }
                    } else {
                        // Create a detached copy to aggregate amounts safely without altering database
                        // state
                        RecipeStepIngredient detachedItem = new RecipeStepIngredient();
                        detachedItem.setIngredient(si.getIngredient());
                        detachedItem.setMeasurement(si.getMeasurement());
                        detachedItem.setAmount(si.getAmount());
                        detachedItem.setIngredientNote(si.getIngredientNote());
                        summaryMap.put(ingredientId, detachedItem);
                    }
                }
            }
        }

        return new java.util.ArrayList<>(summaryMap.values());
    }

    /**
     * Groups all ingredients across all recipe steps by their associated
     * Grocery Category name for the shopping list fragment view.
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, List<RecipeStepIngredient>> getShoppingListIngredients(Recipe recipe) {
        if (recipe == null || recipe.getSteps() == null) {
            return java.util.Collections.emptyMap();
        }

        // Map where Key = Category Name String (e.g., "Produce"), Value = List of
        // ingredients
        java.util.Map<String, List<RecipeStepIngredient>> categoryMap = new java.util.TreeMap<>();

        for (RecipeStep step : recipe.getSteps()) {
            if (step.getStepIngredients() != null) {
                for (RecipeStepIngredient si : step.getStepIngredients()) {
                    // Extract the category name from the ingredient's grocery category association
                    String categoryName = "Uncategorized";
                    if (si.getIngredient() != null && si.getIngredient().getGroceryCategory() != null) {
                        categoryName = si.getIngredient().getGroceryCategory().getCategoryName();
                    }

                    // Enforce inner list initialization if this is the first item in the category
                    categoryMap.computeIfAbsent(categoryName, k -> new java.util.ArrayList<>());

                    // Check for duplicate items within the same category to combine their amounts
                    List<RecipeStepIngredient> currentCategoryList = categoryMap.get(categoryName);
                    RecipeStepIngredient existingMatch = null;

                    for (RecipeStepIngredient existing : currentCategoryList) {
                        if (existing.getIngredient().getIngredientId().equals(si.getIngredient().getIngredientId())) {
                            existingMatch = existing;
                            break;
                        }
                    }

                    if (existingMatch != null) {
                        if (existingMatch.getAmount() != null && si.getAmount() != null) {
                            existingMatch.setAmount(existingMatch.getAmount().add(si.getAmount()));
                        }
                    } else {
                        // Create a detached clone to aggregate amounts safely without altering database
                        // state
                        RecipeStepIngredient detachedItem = new RecipeStepIngredient();
                        detachedItem.setIngredient(si.getIngredient());
                        detachedItem.setMeasurement(si.getMeasurement());
                        detachedItem.setAmount(si.getAmount());
                        detachedItem.setIngredientNote(si.getIngredientNote());
                        currentCategoryList.add(detachedItem);
                    }
                }
            }
        }

        return categoryMap;
    }

    @Transactional
    public void forceRecipePrivateByPublicUuid(UUID publicId) {
        Recipe recipe = recipeRepository.findByPublicId(publicId)
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found with public UUID: " + publicId));

        applyPrivateVisibility(recipe);
    }

    @Transactional
    public void forceRecipePrivateBySequenceId(Integer recipeId) { // Updated type to Integer
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found with ID: " + recipeId));

        applyPrivateVisibility(recipe);
    }

    // Reusable core state modification method
    private void applyPrivateVisibility(Recipe recipe) {
        RecipeVisibility privateVisibility = visibilityRepository.findByVisibilityName("private")
                .orElseThrow(() -> new IllegalStateException(
                        "Required visibility configuration 'private' is missing from lookups."));

        recipe.setRecipeVisibility(privateVisibility);
        recipeRepository.save(recipe);
    }
}
