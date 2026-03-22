package com.arcus.arc1.SetLog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SetLogRepo
        extends JpaRepository<SetLogEntity, Long> {

    /**
     * Find all set logs for a specific workout exercise template, ordered by set number.
     */
    List<SetLogEntity> findByWorkoutExerciseTemplateIdOrderBySetNumberAsc(Long workoutExerciseTemplateId);

    /**
     * Find average reps for a specific exercise by user in the last 30 days using workoutExerciseTemplateId.
     * Joins SetLogEntity to WorkoutExerciseTemplateEntity via workoutExerciseTemplateId.
     */
    @Query("SELECT AVG(sl.reps) FROM SetLogEntity sl " +
           "JOIN WorkoutExerciseTemplateEntity wet ON sl.workoutExerciseTemplateId = wet.id " +
           "WHERE wet.userId = :userId " +
           "AND wet.exerciseLibraryId = :exerciseLibraryId " +
           "AND wet.createdAt >= :thresholdDate")
    Double findAverageRepsForExerciseTemplateLastMonth(Integer userId, Integer exerciseLibraryId, java.time.ZonedDateTime thresholdDate);
}