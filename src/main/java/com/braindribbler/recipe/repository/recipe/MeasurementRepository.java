package com.braindribbler.recipe.repository.recipe;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.braindribbler.recipe.domain.recipe.Measurement;

public interface MeasurementRepository extends JpaRepository<Measurement, Long> {
    Optional<Measurement> findByUnitName(String unitName);
}
