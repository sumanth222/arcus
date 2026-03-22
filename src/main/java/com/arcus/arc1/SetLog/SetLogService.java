package com.arcus.arc1.SetLog;

import com.arcus.arc1.ExerciseLibrary.ExerciseLibraryEntity;
import com.arcus.arc1.ExerciseLibrary.ExerciseLibraryRepo;
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
    private final ExerciseLibraryRepo exerciseLibraryRepo;


    public SetLogService(SetLogRepo setLogRepo,
                        SetEvaluationService setEvaluationService,
                        ExerciseSessionRepo exerciseSessionRepo,
                        WorkoutSessionRepo workoutSessionRepo,
                        UserProfileRepo userProfileRepo,
                         ExerciseLibraryRepo exerciseLibraryRepo) {
        this.setLogRepo = setLogRepo;
        this.setEvaluationService = setEvaluationService;
        this.exerciseSessionRepo = exerciseSessionRepo;
        this.workoutSessionRepo = workoutSessionRepo;
        this.userProfileRepo = userProfileRepo;
        this.exerciseLibraryRepo = exerciseLibraryRepo;
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
        setLog.setWorkoutExerciseTemplateId(request.getExerciseSessionId());
        setLog.setSetNumber(request.getSetNumber());
        setLog.setWeight(request.getWeight());
        setLog.setReps(request.getReps());
        setLogRepo.save(setLog);

        // Update user profile with the logged set data
        updateUserProfileWithSet(request.getExerciseSessionId(), request.getWeight(), request.getReps());

        // Fetch all previous sets for this exercise (by workoutExerciseTemplateId)
        Long templateId = request.getExerciseSessionId();
        System.out.println("Fetching sets for template ID: " + templateId);

        java.util.List<SetLogEntity> allSets = setLogRepo.findByWorkoutExerciseTemplateIdOrderBySetNumberAsc(templateId);

        // Get user level
        ExerciseLibraryEntity exerciseSession = exerciseLibraryRepo.findById(request.getExerciseSessionId()).orElse(null);
        String userLevel = "beginner";
        if (exerciseSession != null) {
            UserProfileEntity profile = userProfileRepo.findByUserId(1L).orElse(null);
            if (profile != null && profile.getCurrentLevel() != null) {
                userLevel = profile.getCurrentLevel().toLowerCase();
            }
        }

        // Determine increment based on user level
        double increment = 2.5;
        if (userLevel.contains("intermediate")) increment = 5.0;
        else if (userLevel.contains("advanced") || userLevel.contains("expert")) increment = 7.5;

        // Find the last set's weight and reps
        double lastWeight = request.getWeight() != null ? request.getWeight() : 0.0;
        int lastReps = request.getReps() != null ? request.getReps() : 0;
        int targetReps = (exerciseSession != null && exerciseSession.getRepMax() > 0)
                ? exerciseSession.getRepMax()
                : lastReps;

        // Analyze performance: if user hit target reps in all sets, recommend increase
        boolean allSetsHitTarget = allSets.stream().allMatch(s -> s.getReps() != null && s.getReps() >= targetReps);
        boolean fatigue = false;
        if (allSets.size() > 1) {
            int firstReps = allSets.get(0).getReps() != null ? allSets.get(0).getReps() : 0;
            int lastSetReps = allSets.get(allSets.size() - 1).getReps() != null ? allSets.get(allSets.size() - 1).getReps() : 0;
            fatigue = (firstReps - lastSetReps) >= 2 || (firstReps > 0 && ((double)(firstReps - lastSetReps) / firstReps) >= 0.15);
        }


        System.out.println("Fatiguw: "+ fatigue);
        double nextSetWeight = lastWeight;
        int nextSetReps = targetReps;
        if (allSets.size() == 1) {
            // First set, no history: keep weight the same, or increase if all reps hit
            if (allSetsHitTarget) {
                nextSetWeight = snapToGymIncrement(lastWeight + increment);
            } else {
                nextSetWeight = lastWeight;
            }
        } else if (!fatigue && allSetsHitTarget) {
            nextSetWeight = snapToGymIncrement(lastWeight + increment);
        } else if (fatigue) {
            nextSetWeight = snapToGymIncrement(Math.max(2.5, lastWeight - increment));
            nextSetReps = Math.max(5, lastReps - 1);
        }

        // Evaluate the set and return feedback with next set recommendation
        SetEvaluationDTO eval = setEvaluationService.evaluateSet(request.getExerciseSessionId(), request.getSetNumber());
        eval.setNextSetWeight(nextSetWeight);
        eval.setNextSetReps(nextSetReps);
        return eval;
    }

    // Snap weight to nearest gym increment (2.5, 5, 7.5, 10, ...)
    private double snapToGymIncrement(double weight) {
        double[] increments = {2.5, 5, 7.5, 10, 12.5, 15, 17.5, 20, 22.5, 25, 27.5, 30, 32.5, 35, 37.5, 40, 42.5, 45, 47.5, 50};
        for (double inc : increments) {
            if (weight <= inc) return inc;
        }
        // If above all, round to nearest 2.5
        return Math.round(weight / 2.5) * 2.5;
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
        setLog.setWorkoutExerciseTemplateId(request.getWorkoutExerciseTemplateId());
        setLog.setSetNumber(request.getSetNumber());
        setLog.setWeight(request.getWeight());
        setLog.setReps(request.getReps());

        setLogRepo.save(setLog);
    }
}