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
}