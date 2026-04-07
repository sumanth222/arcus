package com.arcus.arc1.Nutrition;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NutritionInsightRepo extends JpaRepository<NutritionInsightEntity, Long> {
    /** Most recent insights for a user, newest first */
    List<NutritionInsightEntity> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
}

