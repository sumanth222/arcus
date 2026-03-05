package com.arcus.arc1.ExerciseSession;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExerciseSessionRepo
        extends JpaRepository<ExerciseSessionEntity, Long> {
}