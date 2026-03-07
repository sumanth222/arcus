package com.arcus.arc1.WorkoutSession;

import com.arcus.arc1.dto.WorkoutProgressDTO;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for workout progress tracking.
 * Provides real-time progress data for UI during active workout.
 *
 * Endpoint:
 * - GET /workout/session/{userId}/progress?workoutSessionId={id}
 */
@RestController
@RequestMapping("/workout/session")
public class WorkoutProgressController {

    private final WorkoutProgressService workoutProgressService;

    public WorkoutProgressController(WorkoutProgressService workoutProgressService) {
        this.workoutProgressService = workoutProgressService;
    }

    /**
     * Gets current progress of a workout session.
     * Perfect for displaying live progress during a workout.
     *
     * @param userId User ID
     * @param workoutSessionId Workout session ID
     * @return WorkoutProgressDTO with current progress state
     *
     * Example Response:
     * {
     *   "workoutSessionId": 5,
     *   "userId": 1,
     *   "workoutCompleted": false,
     *   "currentExerciseSessionId": 14,
     *   "currentExerciseName": "Bench Press",
     *   "currentExerciseWeight": 50.0,
     *   "currentExerciseRepMin": 8,
     *   "currentExerciseRepMax": 12,
     *   "currentExerciseSetsPlanned": 3,
     *   "setsCompleted": 2,
     *   "setsRemaining": 1,
     *   "setsCompletedPercentage": 66.67,
     *   "nextExerciseSessionId": 15,
     *   "nextExerciseName": "Barbell Squat",
     *   "nextExerciseWeight": 60.0,
     *   "sessionStatus": "in_progress",
     *   "totalExercises": 4,
     *   "completedExercises": 1
     * }
     */
    @GetMapping("/{userId}/progress")
    public WorkoutProgressDTO getWorkoutProgress(
            @PathVariable Long userId,
            @RequestParam Long workoutSessionId) {
        return workoutProgressService.getWorkoutProgress(userId, workoutSessionId);
    }
}

