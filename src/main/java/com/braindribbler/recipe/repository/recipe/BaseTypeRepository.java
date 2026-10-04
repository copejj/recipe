package com.braindribbler.recipe.repository.recipe;

import org.springframework.data.jpa.repository.JpaRepository;

import com.braindribbler.recipe.domain.recipe.BaseType;

public interface BaseTypeRepository extends JpaRepository<BaseType, Integer> {
}
