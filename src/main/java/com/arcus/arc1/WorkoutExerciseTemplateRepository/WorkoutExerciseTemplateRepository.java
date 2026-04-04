package com.arcus.arc1.WorkoutExerciseTemplateRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkoutExerciseTemplateRepository extends JpaRepository<WorkoutExerciseTemplateEntity, Long> {

    /**
     * Find all template entries for a user on a specific day, most-recently-created first.
     * Used when reusing an existing workout session to avoid creating duplicate template rows.
     */
    List<WorkoutExerciseTemplateEntity> findByUserIdAndDayNumberOrderByCreatedAtDesc(Integer userId, Integer dayNumber);
}

