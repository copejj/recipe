package com.braindribbler.recipe.service.recipe;

import com.braindribbler.recipe.domain.recipe.GroceryCategory;
import com.braindribbler.recipe.repository.recipe.GroceryCategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class GroceryCategoryService {

    private final GroceryCategoryRepository groceryCategoryRepository;

    public GroceryCategoryService(GroceryCategoryRepository groceryCategoryRepository) {
        this.groceryCategoryRepository = groceryCategoryRepository;
    }

    /**
     * Fetches all tracked store layout categories sorted by explicit display
     * sequence metrics. Cached at static speed in memory.
     */
    @Cacheable(value = "groceryCategories")
    @Transactional(readOnly = true)
    public List<GroceryCategory> getAllCategoriesSorted() {
        return groceryCategoryRepository.findAllByOrderByDisplayOrderAsc();
    }

    /**
     * Manually registers a brand new grocery category aisle
     */
    @CacheEvict(value = "groceryCategories", allEntries = true)
    public GroceryCategory createNewCategory(String name, Integer displayOrder) {
        GroceryCategory category = new GroceryCategory();
        category.setCategoryName(name.trim());
        category.setDisplayOrder(displayOrder != null ? displayOrder : 0);
        return groceryCategoryRepository.save(category);
    }

    /**
     * Updates an existing grocery category parameters
     */
    @CacheEvict(value = "groceryCategories", allEntries = true)
    public void updateCategory(Integer id, String name, Integer displayOrder) {
        GroceryCategory category = groceryCategoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with ID: " + id));

        category.setCategoryName(name.trim());
        if (displayOrder != null) {
            category.setDisplayOrder(displayOrder);
        }
        groceryCategoryRepository.save(category);
    }

    /**
     * Bulk updates all grocery categories within a single transaction pipeline
     */
    @CacheEvict(value = "groceryCategories", allEntries = true)
    public void saveAllCategories(List<GroceryCategory> categories) {
        if (categories != null) {
            for (GroceryCategory cat : categories) {
                this.updateCategory(
                        cat.getGroceryCategoryId(),
                        cat.getCategoryName(),
                        cat.getDisplayOrder());
            }
        }
    }

    /**
     * Drops a single grocery category by its identifier key
     */
    @CacheEvict(value = "groceryCategories", allEntries = true)
    public void deleteCategory(Integer id) {
        if (!groceryCategoryRepository.existsById(id)) {
            throw new EntityNotFoundException("Category not found with ID: " + id);
        }
        groceryCategoryRepository.deleteById(id);
    }
}
