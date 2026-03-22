package com.arcus.arc1.WorkoutExerciseTemplateRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkoutExerciseTemplateRepository extends JpaRepository<WorkoutExerciseTemplateEntity, Long> {
    // Add custom query methods if needed
}

