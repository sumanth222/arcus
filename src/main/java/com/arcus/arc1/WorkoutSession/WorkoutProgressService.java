package com.arcus.arc1.WorkoutSession;

import com.arcus.arc1.ExerciseSession.ExerciseSessionEntity;
import com.arcus.arc1.ExerciseSession.ExerciseSessionRepo;
import com.arcus.arc1.SetLog.SetLogRepo;
import com.arcus.arc1.dto.WorkoutProgressDTO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for tracking workout progress.
 * Provides real-time state of a workout session for UI display.
 */
@Service
public class WorkoutProgressService {

    private final WorkoutSessionRepo workoutSessionRepo;
    private final ExerciseSessionRepo exerciseSessionRepo;
    private final SetLogRepo setLogRepo;

    public WorkoutProgressService(WorkoutSessionRepo workoutSessionRepo,
                                 ExerciseSessionRepo exerciseSessionRepo,
                                 SetLogRepo setLogRepo) {
        this.workoutSessionRepo = workoutSessionRepo;
        this.exerciseSessionRepo = exerciseSessionRepo;
        this.setLogRepo = setLogRepo;
    }

    /**
     * Gets current progress of a workout session.
     * Returns information about current exercise, sets completed, and next exercise.
     *
     * @param userId User ID
     * @param workoutSessionId Workout session ID
     * @return WorkoutProgressDTO with current progress state
     */
    public WorkoutProgressDTO getWorkoutProgress(Long userId, Long workoutSessionId) {
        // Get the workout session
        WorkoutSessionEntity workoutSession = workoutSessionRepo.findById(workoutSessionId)
                .orElseThrow(() -> new RuntimeException("Workout session not found"));

        // Security check: user must own this workout
        if (!workoutSession.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: User does not own this workout");
        }

        // Fetch all exercises in this workout ordered by ID
        List<ExerciseSessionEntity> allExercises = exerciseSessionRepo
                .findByWorkoutSessionId(workoutSessionId);

        if (allExercises.isEmpty()) {
            throw new RuntimeException("Workout has no exercises");
        }

        // Find first incomplete exercise (current exercise)
        ExerciseSessionEntity currentExercise = null;
        int currentExerciseIndex = -1;

        for (int i = 0; i < allExercises.size(); i++) {
            ExerciseSessionEntity exercise = allExercises.get(i);
            int setsCompleted = (int) setLogRepo.findByExerciseSessionIdOrderBySetNumberAsc(exercise.getId()).size();

            // If sets completed < sets planned, this is the current exercise
            if (setsCompleted < exercise.getSets()) {
                currentExercise = exercise;
                currentExerciseIndex = i;
                break;
            }
        }

        // If no incomplete exercise found, workout is completed
        if (currentExercise == null) {
            // Return progress with last exercise as current and marked completed
            ExerciseSessionEntity lastExercise = allExercises.get(allExercises.size() - 1);
            int lastExerciseSetsCompleted = (int) setLogRepo
                    .findByExerciseSessionIdOrderBySetNumberAsc(lastExercise.getId()).size();

            return new WorkoutProgressDTO(
                    workoutSessionId,
                    userId,
                    true, // workoutCompleted
                    lastExercise.getId(),
                    lastExercise.getExerciseName(),
                    lastExercise.getTargetWeight(),
                    lastExercise.getRepMin(),
                    lastExercise.getRepMax(),
                    lastExercise.getSets(),
                    lastExerciseSetsCompleted,
                    0, // setsRemaining
                    100.0, // setsCompletedPercentage
                    null, // nextExerciseSessionId
                    null, // nextExerciseName
                    null, // nextExerciseWeight
                    "completed",
                    allExercises.size(),
                    allExercises.size()
            );
        }

        // Get current exercise details
        int currentSetsCompleted = (int) setLogRepo
                .findByExerciseSessionIdOrderBySetNumberAsc(currentExercise.getId()).size();
        int currentSetsRemaining = currentExercise.getSets() - currentSetsCompleted;
        double setsCompletedPercentage = (currentSetsCompleted * 100.0) / currentExercise.getSets();

        // Get next exercise
        ExerciseSessionEntity nextExercise = null;
        if (currentExerciseIndex < allExercises.size() - 1) {
            nextExercise = allExercises.get(currentExerciseIndex + 1);
        }

        // Calculate completed exercises (exercises with all sets done)
        int completedExercises = 0;
        for (int i = 0; i < currentExerciseIndex; i++) {
            ExerciseSessionEntity exercise = allExercises.get(i);
            int setsCompleted = (int) setLogRepo.findByExerciseSessionIdOrderBySetNumberAsc(exercise.getId()).size();
            if (setsCompleted >= exercise.getSets()) {
                completedExercises++;
            }
        }

        return new WorkoutProgressDTO(
                workoutSessionId,
                userId,
                false, // workoutCompleted
                currentExercise.getId(),
                currentExercise.getExerciseName(),
                currentExercise.getTargetWeight(),
                currentExercise.getRepMin(),
                currentExercise.getRepMax(),
                currentExercise.getSets(),
                currentSetsCompleted,
                currentSetsRemaining,
                setsCompletedPercentage,
                nextExercise != null ? nextExercise.getId() : null,
                nextExercise != null ? nextExercise.getExerciseName() : null,
                nextExercise != null ? nextExercise.getTargetWeight() : null,
                "in_progress",
                allExercises.size(),
                completedExercises
        );
    }
}

