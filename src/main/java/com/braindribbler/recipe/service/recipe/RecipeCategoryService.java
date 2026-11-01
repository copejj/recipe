package com.braindribbler.recipe.service.recipe;

import com.braindribbler.recipe.domain.recipe.RecipeCategory;
import com.braindribbler.recipe.repository.recipe.RecipeCategoryRepository;
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

    @Transactional(readOnly = true)
    public List<RecipeCategory> getAllCategories() {
        return recipeCategoryRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Optional<RecipeCategory> getCategoryById(Integer id) {
        return recipeCategoryRepository.findById(id);
    }

    public RecipeCategory saveCategory(RecipeCategory category) {
        if (category.getName() != null) {
            category.setName(category.getName().trim());
        }
        return recipeCategoryRepository.save(category);
    }

    public void deleteCategoryById(Integer id) {
        recipeCategoryRepository.deleteById(id);
    }
}
