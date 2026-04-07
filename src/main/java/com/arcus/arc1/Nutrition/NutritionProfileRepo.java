package com.arcus.arc1.Nutrition;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NutritionProfileRepo extends JpaRepository<NutritionProfileEntity, Long> {
    Optional<NutritionProfileEntity> findByUserId(Long userId);
}

