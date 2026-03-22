package com.arcus.arc1.ExerciseSession;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExerciseSessionRepo
        extends JpaRepository<ExerciseSessionEntity, Long> {

    /**
     * Find all exercises in a specific workout session.
     */
    List<ExerciseSessionEntity> findByWorkoutSessionId(Long workoutSessionId);

    /**
     * Find a specific exercise by workout session and name.
     */
    Optional<ExerciseSessionEntity> findByWorkoutSessionIdAndExerciseName(Long workoutSessionId, String exerciseName);

    /**
     * Find all exercise sessions for a user across all workouts.
     */
    @Query("SELECT es FROM ExerciseSessionEntity es " +
           "JOIN WorkoutSessionEntity ws ON es.workoutSessionId = ws.id " +
           "WHERE ws.userId = :userId " +
           "ORDER BY ws.createdAt DESC")
    List<ExerciseSessionEntity> findByUserId(Long userId);

    /**
     * Find recent exercise sessions for a user, excluding the current workout session.
     * Only returns sessions that actually have set logs recorded (via EXISTS subquery),
     * so unlogged sessions don't pollute history with averageReps = 0.
     */
    @Query("SELECT es FROM ExerciseSessionEntity es " +
           "JOIN WorkoutSessionEntity ws ON es.workoutSessionId = ws.id " +
           "WHERE ws.userId = :userId AND es.exerciseName = :exerciseName " +
           "AND ws.id <> :excludeWorkoutSessionId " +
           "ORDER BY ws.createdAt DESC")
    List<ExerciseSessionEntity> findRecentExercisesByUserAndName(Long userId, String exerciseName, Long excludeWorkoutSessionId);
}