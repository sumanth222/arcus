package com.arcus.arc1.SetLog;

import com.arcus.arc1.dto.SetLogDTO;
import com.arcus.arc1.dto.SetEvaluationDTO;
import com.arcus.arc1.ExerciseSession.ExerciseSessionEntity;
import com.arcus.arc1.ExerciseSession.ExerciseSessionRepo;
import com.arcus.arc1.UserProfile.UserProfileEntity;
import com.arcus.arc1.UserProfile.UserProfileRepo;
import com.arcus.arc1.WorkoutSession.WorkoutSessionEntity;
import com.arcus.arc1.WorkoutSession.WorkoutSessionRepo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service for managing set logs and coordinating with evaluation logic.
 * Also updates user profile statistics when sets are logged.
 */
@Service
public class SetLogService {

    private final SetLogRepo setLogRepo;
    private final SetEvaluationService setEvaluationService;
    private final ExerciseSessionRepo exerciseSessionRepo;
    private final WorkoutSessionRepo workoutSessionRepo;
    private final UserProfileRepo userProfileRepo;

    public SetLogService(SetLogRepo setLogRepo,
                        SetEvaluationService setEvaluationService,
                        ExerciseSessionRepo exerciseSessionRepo,
                        WorkoutSessionRepo workoutSessionRepo,
                        UserProfileRepo userProfileRepo) {
        this.setLogRepo = setLogRepo;
        this.setEvaluationService = setEvaluationService;
        this.exerciseSessionRepo = exerciseSessionRepo;
        this.workoutSessionRepo = workoutSessionRepo;
        this.userProfileRepo = userProfileRepo;
    }

    /**
     * Saves a set log and returns an evaluation of the set performance.
     * Also updates the user profile with the logged weight and reps.
     *
     * @param request The SetLogDTO containing set information
     * @return SetEvaluationDTO with fatigue detection and rest recommendations
     */
    public SetEvaluationDTO saveLogAndEvaluate(SetLogDTO request) {
        SetLogEntity setLog = new SetLogEntity();
        setLog.setExerciseSessionId(request.getExerciseSessionId());
        setLog.setSetNumber(request.getSetNumber());
        setLog.setWeight(request.getWeight());
        setLog.setReps(request.getReps());

        setLogRepo.save(setLog);

        // Update user profile with the logged set data
        updateUserProfileWithSet(request.getExerciseSessionId(), request.getWeight(), request.getReps());

        // Evaluate the set and return feedback
        return setEvaluationService.evaluateSet(request.getExerciseSessionId(), request.getSetNumber());
    }

    /**
     * Updates user profile with set data.
     * Calculates weight × reps and adds to user's total weight lifted.
     *
     * @param exerciseSessionId The exercise session ID
     * @param weight The weight lifted
     * @param reps The number of reps performed
     */
    private void updateUserProfileWithSet(Long exerciseSessionId, Double weight, Integer reps) {
        try {
            // Get the exercise session to find the workout session
            ExerciseSessionEntity exerciseSession = exerciseSessionRepo.findById(exerciseSessionId)
                    .orElse(null);

            if (exerciseSession == null) {
                return;
            }

            // Get the workout session to find the user
            WorkoutSessionEntity workoutSession = workoutSessionRepo.findById(exerciseSession.getWorkoutSessionId())
                    .orElse(null);

            if (workoutSession == null) {
                return;
            }

            // Get the user profile and update it
            UserProfileEntity profile = userProfileRepo.findByUserId(workoutSession.getUserId())
                    .orElse(null);

            if (profile != null) {
                // Add weight × reps to total weight lifted
                Double weightLifted = weight * reps;
                profile.setTotalWeightLifted(profile.getTotalWeightLifted() + weightLifted);
                profile.setLastWorkoutDate(LocalDateTime.now());
                profile.setLastUpdatedAt(LocalDateTime.now());

                userProfileRepo.save(profile);
            }
        } catch (Exception e) {
            // Log error but don't fail set logging if profile update fails
            System.err.println("Error updating user profile with set data: " + e.getMessage());
        }
    }

    /**
     * Legacy method for backwards compatibility - just saves without evaluation.
     *
     * @param request The SetLogDTO containing set information
     */
    public void saveLog(SetLogDTO request) {

        SetLogEntity setLog = new SetLogEntity();
        setLog.setExerciseSessionId(request.getExerciseSessionId());
        setLog.setSetNumber(request.getSetNumber());
        setLog.setWeight(request.getWeight());
        setLog.setReps(request.getReps());

        setLogRepo.save(setLog);
    }
}