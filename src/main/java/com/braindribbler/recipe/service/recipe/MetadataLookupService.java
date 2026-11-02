package com.braindribbler.recipe.service.recipe;

import com.braindribbler.recipe.domain.recipe.AccessLevel;
import com.braindribbler.recipe.domain.recipe.BaseType;
import com.braindribbler.recipe.domain.recipe.Measurement;
import com.braindribbler.recipe.domain.recipe.RecipeVisibility;
import com.braindribbler.recipe.repository.recipe.AccessLevelRepository;
import com.braindribbler.recipe.repository.recipe.BaseTypeRepository;
import com.braindribbler.recipe.repository.recipe.MeasurementRepository;
import com.braindribbler.recipe.repository.recipe.RecipeVisibilityRepository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MetadataLookupService {

    private final AccessLevelRepository accessLevelRepository;
    private final BaseTypeRepository baseTypeRepository;
    private final MeasurementRepository measurementRepository;
    private final RecipeVisibilityRepository recipeVisibilityRepository;

    public MetadataLookupService(AccessLevelRepository accessLevelRepository,
                                 BaseTypeRepository baseTypeRepository,
                                 MeasurementRepository measurementRepository,
                                 RecipeVisibilityRepository recipeVisibilityRepository) {
        this.accessLevelRepository = accessLevelRepository;
        this.baseTypeRepository = baseTypeRepository;
        this.measurementRepository = measurementRepository;
        this.recipeVisibilityRepository = recipeVisibilityRepository;
    }

    @Cacheable(value = "accessLevels")
    public List<AccessLevel> getAllAccessLevels() {
        return accessLevelRepository.findAll();
    }

    @Cacheable(value = "baseTypes")
    public List<BaseType> getAllBaseTypes() {
        return baseTypeRepository.findAll();
    }

    @Cacheable(value = "measurements")
    public List<Measurement> getAllMeasurements() {
        return measurementRepository.findAll();
    }

    @Cacheable(value = "visibilities")
    public List<RecipeVisibility> getAllVisibilities() {
        return recipeVisibilityRepository.findAll();
    }

    /**
     * Admin modification utility hook to flush all metadata lookups if configuration tables get altered.
     */
    @CacheEvict(value = {"accessLevels", "baseTypes", "measurements", "visibilities"}, allEntries = true)
    @Transactional
    public void flushAllMetadataCaches() {
        // Intentionally empty method designed purely to clear cache layers via AOP aspect interceptors
    }
}
