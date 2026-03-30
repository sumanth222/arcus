package com.arcus.arc1.WorkoutSession;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkoutSessionRepo
        extends JpaRepository<WorkoutSessionEntity, Long> {

    List<WorkoutSessionEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<WorkoutSessionEntity> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<WorkoutSessionEntity> findTopByUserIdAndCompletedTrueOrderByCreatedAtDesc(Long userId);

    Optional<WorkoutSessionEntity> findTopByUserIdAndCompletedFalseOrderByCreatedAtDesc(Long userId);

    /**
     * All completed workout sessions for a user for a specific template, most-recent-first.
     * Used to compare total weights between the last two occurrences of the same template.
     */
    List<WorkoutSessionEntity> findByUserIdAndTemplateIdAndCompletedTrueOrderByCreatedAtDesc(Long userId, Long templateId);

    /**
     * Fetch at most two most-recent completed sessions for a user and template.
     * This limits DB work when we only need the last and the previous occurrence.
     */
    List<WorkoutSessionEntity> findTop2ByUserIdAndTemplateIdAndCompletedTrueOrderByCreatedAtDesc(Long userId, Long templateId);
}