package com.arcus.arc1.SetLog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SetLogRepo
        extends JpaRepository<SetLogEntity, Long> {

    List<SetLogEntity> findByExerciseSessionIdOrderBySetNumberAsc(Long exerciseSessionId);

    /**
     * Find all set logs for a specific exercise across multiple sessions.
     * Used for calculating performance history.
     */
    @Query("SELECT sl FROM SetLogEntity sl " +
           "JOIN ExerciseSessionEntity es ON sl.exerciseSessionId = es.id " +
           "WHERE es.workoutSessionId IN " +
           "(SELECT ws.id FROM WorkoutSessionEntity ws WHERE ws.userId = :userId) " +
           "ORDER BY sl.exerciseSessionId DESC, sl.setNumber ASC")
    List<SetLogEntity> findSetLogsByUserOrderByRecent(Long userId);

    /**
     * Find average reps for a specific exercise by user in the last 30 days.
     * Uses a threshold date parameter to avoid temporal arithmetic issues.
     */
    @Query("SELECT AVG(sl.reps) FROM SetLogEntity sl " +
           "JOIN ExerciseSessionEntity es ON sl.exerciseSessionId = es.id " +
           "JOIN WorkoutSessionEntity ws ON es.workoutSessionId = ws.id " +
           "WHERE ws.userId = :userId " +
           "AND es.exerciseName = :exerciseName " +
           "AND ws.createdAt >= :thresholdDate")
    Double findAverageRepsForExerciseLastMonth(Long userId, String exerciseName, LocalDateTime thresholdDate);
}