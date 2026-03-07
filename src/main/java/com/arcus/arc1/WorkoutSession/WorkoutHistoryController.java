package com.arcus.arc1.WorkoutSession;

import com.arcus.arc1.dto.WorkoutHistoryDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for workout history endpoints.
 * Provides APIs for retrieving user workout history for charts and analytics.
 *
 * Endpoints:
 * - GET /workout/history/{userId} - Get complete workout history
 * - GET /workout/history/{userId}/recent - Get recent workouts with limit
 * - GET /workout/history/{userId}/workout/{workoutSessionId} - Get single workout details
 */
@RestController
@RequestMapping("/workout/history")
public class WorkoutHistoryController {

    private final WorkoutHistoryService workoutHistoryService;

    public WorkoutHistoryController(WorkoutHistoryService workoutHistoryService) {
        this.workoutHistoryService = workoutHistoryService;
    }

    /**
     * Gets complete workout history for a user.
     * Returns all workouts ordered by most recent first.
     * Includes exercise and set details for each workout.
     *
     * @param userId User ID
     * @return List of WorkoutHistoryDTO objects
     *
     * Example Response:
     * [
     *   {
     *     "workoutSessionId": 5,
     *     "createdAt": "2026-03-06T15:30:00",
     *     "completed": true,
     *     "totalExercises": 4,
     *     "totalSets": 12,
     *     "totalWeightLifted": 2500.0,
     *     "exercises": [
     *       {
     *         "exerciseSessionId": 14,
     *         "exerciseName": "Bench Press",
     *         "targetWeight": 50.0,
     *         "repMin": 8,
     *         "repMax": 12,
     *         "sets": 3,
     *         "tempo": "3-1-1",
     *         "setsCompleted": 3,
     *         "averageReps": 9.33,
     *         "totalWeightForExercise": 450.0,
     *         "setLogs": [
     *           {"setLogId": 1, "setNumber": 1, "weight": 50.0, "reps": 10},
     *           {"setLogId": 2, "setNumber": 2, "weight": 50.0, "reps": 9},
     *           {"setLogId": 3, "setNumber": 3, "weight": 50.0, "reps": 9}
     *         ]
     *       }
     *     ]
     *   }
     * ]
     */
    @GetMapping("/{userId}")
    public List<WorkoutHistoryDTO> getUserWorkoutHistory(@PathVariable Long userId) {
        return workoutHistoryService.getUserWorkoutHistory(userId);
    }

    /**
     * Gets recent workout history for a user with optional limit.
     * Useful for showing last N workouts on dashboard or charts.
     *
     * @param userId User ID
     * @param limit Number of recent workouts to return (default: 10)
     * @return List of recent WorkoutHistoryDTO objects
     *
     * Example: GET /workout/history/1/recent?limit=5
     * Returns last 5 workouts
     */
    @GetMapping("/{userId}/recent")
    public List<WorkoutHistoryDTO> getRecentWorkoutHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        return workoutHistoryService.getUserWorkoutHistoryLimited(userId, limit);
    }

    /**
     * Gets details of a single workout.
     * Includes all exercises and set logs for that workout.
     *
     * @param userId User ID
     * @param workoutSessionId Workout session ID
     * @return WorkoutHistoryDTO with complete details or null if not found
     *
     * Example: GET /workout/history/1/workout/5
     * Returns complete details for workout session 5
     */
    @GetMapping("/{userId}/workout/{workoutSessionId}")
    public WorkoutHistoryDTO getSingleWorkout(
            @PathVariable Long userId,
            @PathVariable Long workoutSessionId) {
        return workoutHistoryService.getSingleWorkout(userId, workoutSessionId);
    }
}

