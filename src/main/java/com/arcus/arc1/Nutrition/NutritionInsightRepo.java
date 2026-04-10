package com.arcus.arc1.Nutrition;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NutritionInsightRepo extends JpaRepository<NutritionInsightEntity, Long> {
    /** Most recent insights for a user, newest first */
    List<NutritionInsightEntity> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Used for upsert: find the most recent insight for a user whose type starts with
     * the given prefix (e.g. "CONSISTENCY_" or "POST_WORKOUT_").
     */
    Optional<NutritionInsightEntity> findTopByUserIdAndTypeStartingWithOrderByCreatedAtDesc(
            Long userId, String typePrefix);
}

