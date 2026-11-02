package com.braindribbler.recipe.service.recipe;

import com.braindribbler.recipe.domain.recipe.RecipeCategory;
import com.braindribbler.recipe.repository.recipe.RecipeCategoryRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RecipeCategoryService {

    private final RecipeCategoryRepository recipeCategoryRepository;

    public RecipeCategoryService(RecipeCategoryRepository recipeCategoryRepository) {
        this.recipeCategoryRepository = recipeCategoryRepository;
    }

    /**
     * Subsequent requests hit fast in-memory concurrent maps instead of hitting Postgres.
     */
    @Cacheable(value = "recipeCategories")
    @Transactional(readOnly = true)
    public List<RecipeCategory> getAllCategories() {
        return recipeCategoryRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Optional<RecipeCategory> getCategoryById(Integer id) {
        return recipeCategoryRepository.findById(id);
    }

    /**
     * Evicts the entire cache region whenever a category is saved, modified, or added.
     */
    @CacheEvict(value = "recipeCategories", allEntries = true)
    public RecipeCategory saveCategory(RecipeCategory category) {
        if (category.getName() != null) {
            category.setName(category.getName().trim());
        }
        return recipeCategoryRepository.save(category);
    }

    /**
     * Evicts the cache region completely when a category gets deleted.
     */
    @CacheEvict(value = "recipeCategories", allEntries = true)
    public void deleteCategoryById(Integer id) {
        recipeCategoryRepository.deleteById(id);
    }
}
