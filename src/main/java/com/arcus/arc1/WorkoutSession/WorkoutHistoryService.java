package com.arcus.arc1.WorkoutSession;

import com.arcus.arc1.ExerciseSession.ExerciseSessionEntity;
import com.arcus.arc1.ExerciseSession.ExerciseSessionRepo;
import com.arcus.arc1.SetLog.SetLogEntity;
import com.arcus.arc1.SetLog.SetLogRepo;
import com.arcus.arc1.dto.WorkoutHistoryDTO;
import com.arcus.arc1.dto.ExerciseHistoryDTO;
import com.arcus.arc1.dto.SetHistoryDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for retrieving user workout history.
 * Provides detailed workout data for charts and UI display.
 */
@Service
public class WorkoutHistoryService {

    private final WorkoutSessionRepo workoutSessionRepo;
    private final ExerciseSessionRepo exerciseSessionRepo;
    private final SetLogRepo setLogRepo;

    public WorkoutHistoryService(WorkoutSessionRepo workoutSessionRepo,
                                ExerciseSessionRepo exerciseSessionRepo,
                                SetLogRepo setLogRepo) {
        this.workoutSessionRepo = workoutSessionRepo;
        this.exerciseSessionRepo = exerciseSessionRepo;
        this.setLogRepo = setLogRepo;
    }

    /**
     * Gets complete workout history for a user.
     * Includes all workouts with exercise and set details.
     *
     * @param userId User ID
     * @return List of WorkoutHistoryDTO ordered by most recent first
     */
    public List<WorkoutHistoryDTO> getUserWorkoutHistory(Long userId) {
        // Fetch all workouts for user
        List<WorkoutSessionEntity> workouts = workoutSessionRepo.findByUserIdOrderByCreatedAtDesc(userId);

        List<WorkoutHistoryDTO> historyList = new ArrayList<>();

        for (WorkoutSessionEntity workout : workouts) {
            // Fetch exercises for this workout
            List<ExerciseSessionEntity> exercises = exerciseSessionRepo.findByWorkoutSessionId(workout.getId());

            int totalSets = 0;
            double totalWeightLifted = 0.0;
            List<ExerciseHistoryDTO> exerciseHistory = new ArrayList<>();

            for (ExerciseSessionEntity exercise : exercises) {
                // Fetch set logs for this exercise
                List<SetLogEntity> setLogs = new ArrayList<>();
                int setsCompleted = setLogs.size();
                double averageReps = 0.0;
                double exerciseTotalWeight = 0.0;

                if (!setLogs.isEmpty()) {
                    // Calculate average reps and total weight
                    double totalReps = 0;
                    for (SetLogEntity setLog : setLogs) {
                        totalReps += setLog.getReps();
                        exerciseTotalWeight += setLog.getWeight() * setLog.getReps();
                    }
                    averageReps = totalReps / setLogs.size();
                }

                totalSets += exercise.getSets();
                totalWeightLifted += exerciseTotalWeight;

                // Build set history
                List<SetHistoryDTO> setHistory = new ArrayList<>();
                for (SetLogEntity setLog : setLogs) {
                    setHistory.add(new SetHistoryDTO(
                            setLog.getId(),
                            setLog.getSetNumber(),
                            setLog.getWeight(),
                            setLog.getReps()
                    ));
                }

                // Build exercise history
                ExerciseHistoryDTO exerciseHistoryDTO = new ExerciseHistoryDTO(
                        exercise.getId(),
                        exercise.getExerciseName(),
                        exercise.getTargetWeight(),
                        exercise.getRepMin(),
                        exercise.getRepMax(),
                        exercise.getSets(),
                        exercise.getTempo(),
                        setsCompleted,
                        averageReps,
                        exerciseTotalWeight
                );
                exerciseHistoryDTO.setSetLogs(setHistory);
                exerciseHistory.add(exerciseHistoryDTO);
            }

            // Build workout history
            WorkoutHistoryDTO workoutHistory = new WorkoutHistoryDTO(
                    workout.getId(),
                    workout.getCreatedAt(),
                    workout.isCompleted(),
                    exercises.size(),
                    totalSets,
                    totalWeightLifted
            );
            workoutHistory.setExercises(exerciseHistory);
            historyList.add(workoutHistory);
        }

        return historyList;
    }

    /**
     * Gets workout history with limit (for pagination or recent workouts).
     *
     * @param userId User ID
     * @param limit Number of workouts to return
     * @return List of recent WorkoutHistoryDTO
     */
    public List<WorkoutHistoryDTO> getUserWorkoutHistoryLimited(Long userId, int limit) {
        List<WorkoutHistoryDTO> fullHistory = getUserWorkoutHistory(userId);

        // Return only the first 'limit' items
        if (fullHistory.size() <= limit) {
            return fullHistory;
        }

        return fullHistory.subList(0, limit);
    }

    /**
     * Gets a single workout with all details.
     *
     * @param userId User ID
     * @param workoutSessionId Workout session ID
     * @return WorkoutHistoryDTO or null if not found
     */
    public WorkoutHistoryDTO getSingleWorkout(Long userId, Long workoutSessionId) {
        WorkoutSessionEntity workout = workoutSessionRepo.findById(workoutSessionId).orElse(null);

        if (workout == null || !workout.getUserId().equals(userId)) {
            return null; // Security: only return if user owns this workout
        }

        // Build the same way as in getUserWorkoutHistory
        List<ExerciseSessionEntity> exercises = exerciseSessionRepo.findByWorkoutSessionId(workout.getId());

        int totalSets = 0;
        double totalWeightLifted = 0.0;
        List<ExerciseHistoryDTO> exerciseHistory = new ArrayList<>();

        for (ExerciseSessionEntity exercise : exercises) {
            List<SetLogEntity> setLogs = new ArrayList<>();

            int setsCompleted = setLogs.size();
            double averageReps = 0.0;
            double exerciseTotalWeight = 0.0;

            if (!setLogs.isEmpty()) {
                double totalReps = 0;
                for (SetLogEntity setLog : setLogs) {
                    totalReps += setLog.getReps();
                    exerciseTotalWeight += setLog.getWeight() * setLog.getReps();
                }
                averageReps = totalReps / setLogs.size();
            }

            totalSets += exercise.getSets();
            totalWeightLifted += exerciseTotalWeight;

            List<SetHistoryDTO> setHistory = new ArrayList<>();
            for (SetLogEntity setLog : setLogs) {
                setHistory.add(new SetHistoryDTO(
                        setLog.getId(),
                        setLog.getSetNumber(),
                        setLog.getWeight(),
                        setLog.getReps()
                ));
            }

            ExerciseHistoryDTO exerciseHistoryDTO = new ExerciseHistoryDTO(
                    exercise.getId(),
                    exercise.getExerciseName(),
                    exercise.getTargetWeight(),
                    exercise.getRepMin(),
                    exercise.getRepMax(),
                    exercise.getSets(),
                    exercise.getTempo(),
                    setsCompleted,
                    averageReps,
                    exerciseTotalWeight
            );
            exerciseHistoryDTO.setSetLogs(setHistory);
            exerciseHistory.add(exerciseHistoryDTO);
        }

        WorkoutHistoryDTO workoutHistory = new WorkoutHistoryDTO(
                workout.getId(),
                workout.getCreatedAt(),
                workout.isCompleted(),
                exercises.size(),
                totalSets,
                totalWeightLifted
        );
        workoutHistory.setExercises(exerciseHistory);

        return workoutHistory;
    }
}

