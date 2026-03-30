package com.arcus.arc1.SetLog;

import com.arcus.arc1.ExerciseLibrary.ExerciseLibraryEntity;
import com.arcus.arc1.ExerciseLibrary.ExerciseLibraryRepo;
import com.arcus.arc1.WorkoutExerciseTemplateRepository.WorkoutExerciseTemplateEntity;
import com.arcus.arc1.WorkoutExerciseTemplateRepository.WorkoutExerciseTemplateRepository;
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
    private final WorkoutExerciseTemplateRepository workoutExerciseTemplateRepository;

    public SetLogService(SetLogRepo setLogRepo,
                        SetEvaluationService setEvaluationService,
                        ExerciseSessionRepo exerciseSessionRepo,
                        WorkoutSessionRepo workoutSessionRepo,
                        UserProfileRepo userProfileRepo,
                        ExerciseLibraryRepo exerciseLibraryRepo,
                        WorkoutExerciseTemplateRepository workoutExerciseTemplateRepository) {
        this.setLogRepo = setLogRepo;
        this.setEvaluationService = setEvaluationService;
        this.exerciseSessionRepo = exerciseSessionRepo;
        this.workoutSessionRepo = workoutSessionRepo;
        this.userProfileRepo = userProfileRepo;
        this.exerciseLibraryRepo = exerciseLibraryRepo;
        this.workoutExerciseTemplateRepository = workoutExerciseTemplateRepository;
    }

    /**
     * Saves a set log and returns an evaluation of the set performance.
     * Also updates the user profile with the logged weight and reps.
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

        Long templateId = request.getExerciseSessionId();

        // ── Correct lookup chain: template → exercise library ──────────────────
        WorkoutExerciseTemplateEntity template =
                workoutExerciseTemplateRepository.findById(templateId).orElse(null);

        ExerciseLibraryEntity exerciseLib = null;
        if (template != null && template.getExerciseLibraryId() != null) {
            exerciseLib = exerciseLibraryRepo
                    .findById(template.getExerciseLibraryId().longValue())
                    .orElse(null);
        }

        // ── Resolve actual user level ──────────────────────────────────────────
        String userLevel = "beginner";
        if (template != null && template.getUserId() != null) {
            UserProfileEntity profile =
                    userProfileRepo.findByUserId(template.getUserId().longValue()).orElse(null);
            if (profile != null && profile.getCurrentLevel() != null) {
                userLevel = profile.getCurrentLevel().toLowerCase();
            }
        }

        // ── Equipment & category metadata ──────────────────────────────────────
        String equipment = exerciseLib != null ? exerciseLib.getEquipment() : "barbell";
        String category  = exerciseLib != null ? exerciseLib.getCategory()  : "compound";

        // ── Fetch all sets for this exercise so far ────────────────────────────
        java.util.List<SetLogEntity> allSets =
                setLogRepo.findByWorkoutExerciseTemplateIdOrderBySetNumberAsc(templateId);

        int targetReps = (exerciseLib != null && exerciseLib.getRepMax() > 0)
                ? exerciseLib.getRepMax() : (request.getReps() != null ? request.getReps() : 8);

        double lastWeight = request.getWeight() != null ? request.getWeight() : 0.0;
        int    lastReps   = request.getReps()   != null ? request.getReps()   : 0;

        // ── Fatigue detection ──────────────────────────────────────────────────
        boolean fatigue = false;
        if (allSets.size() > 1) {
            int firstReps   = allSets.get(0).getReps() != null ? allSets.get(0).getReps() : 0;
            int lastSetReps = allSets.get(allSets.size() - 1).getReps() != null
                    ? allSets.get(allSets.size() - 1).getReps() : 0;
            fatigue = (firstReps - lastSetReps) >= 2
                    || (firstReps > 0 && ((double)(firstReps - lastSetReps) / firstReps) >= 0.15);
        }

        boolean allSetsHitTarget = allSets.stream()
                .allMatch(s -> s.getReps() != null && s.getReps() >= targetReps);

        // ── Equipment + level aware increment ──────────────────────────────────
        double increment = calculateIncrement(equipment, userLevel, category);

        // ── Next-set recommendation ────────────────────────────────────────────
        double nextSetWeight;
        int    nextSetReps = targetReps;

        if (allSets.size() == 1) {
            // Very first set logged: only reward if they crushed the target reps
            nextSetWeight = allSetsHitTarget
                    ? snapToGymIncrement(lastWeight + increment, equipment)
                    : lastWeight;
        } else if (!fatigue && allSetsHitTarget) {
            nextSetWeight = snapToGymIncrement(lastWeight + increment, equipment);
        } else if (fatigue) {
            // Drop by one step, never below equipment minimum
            double minWeight = minimumWeight(equipment);
            nextSetWeight = snapToGymIncrement(
                    Math.max(minWeight, lastWeight - increment), equipment);
            nextSetReps   = Math.max(5, lastReps - 1);
        } else {
            // Partially hit target — hold the same weight
            nextSetWeight = lastWeight;
        }

        SetEvaluationDTO eval = setEvaluationService.evaluateSet(
                request.getExerciseSessionId(), request.getSetNumber());
        eval.setNextSetWeight(nextSetWeight);
        eval.setNextSetReps(nextSetReps);
        return eval;
    }

    // ── Equipment + level + category aware increment ───────────────────────────
    /**
     * Returns the appropriate weight increment based on equipment type,
     * user level, and exercise category (compound vs isolation).
     *
     * Equipment rules:
     *   dumbbell      – weights come in fixed 2.5 kg steps; jump one step for beginners,
     *                   two steps (5 kg) for intermediate/advanced
     *   barbell /
     *   smith_machine /
     *   ez_bar        – can add 1.25 kg micro-plates → 2.5 kg total is smallest step;
     *                   compound lifts allow larger jumps at higher levels
     *   cable         – stack pins go in 2.5 kg steps
     *   machine       – plates/pins go in 5 kg steps; minimum increment 5 kg
     *   bodyweight    – no weight increment; progression is reps-only
     */
    private double calculateIncrement(String equipment, String userLevel, String category) {
        String eq  = equipment != null ? equipment.toLowerCase().trim() : "barbell";
        String lvl = userLevel  != null ? userLevel.toLowerCase()       : "beginner";

        boolean isAdvanced     = lvl.contains("advanced") || lvl.contains("expert");
        boolean isIntermediate = lvl.contains("intermediate");
        boolean isIsolation    = "isolation".equalsIgnoreCase(category);

        switch (eq) {
            case "dumbbell":
                // Fixed-weight increments: 5→7.5→10→12.5 ... (2.5 kg per dumbbell)
                // Beginners: one step (2.5 kg); intermediate/advanced: can jump two steps (5 kg)
                if (isAdvanced)     return isIsolation ? 2.5 : 5.0;
                if (isIntermediate) return 2.5;
                return 2.5; // beginner: smallest safe step on dumbbells

            case "barbell":
            case "smith_machine":
            case "ez_bar":
                // Can load in 1.25 kg increments → 2.5 kg total minimum
                // Compound lifts allow 5 kg jumps at higher levels
                if (isAdvanced)     return isIsolation ? 2.5 : 5.0;
                if (isIntermediate) return isIsolation ? 2.5 : 2.5;
                return 2.5;

            case "cable":
                // Stack pins typically 2.5 kg steps
                if (isAdvanced)     return 5.0;
                if (isIntermediate) return 2.5;
                return 2.5;

            case "machine":
                // Plate/pin stacks in 5 kg increments (most gyms)
                if (isAdvanced)     return 10.0;
                if (isIntermediate) return  5.0;
                return 5.0;

            case "bodyweight":
                return 0; // progression is reps-only; no weight increment

            default:
                if (isAdvanced)     return 5.0;
                if (isIntermediate) return 2.5;
                return 2.5;
        }
    }

    /**
     * Snaps a weight value to the nearest valid gym increment for the given equipment.
     *   dumbbell / barbell / cable / default → nearest 2.5 kg
     *   machine                              → nearest 5 kg
     */
    private double snapToGymIncrement(double weight, String equipment) {
        String eq = equipment != null ? equipment.toLowerCase().trim() : "barbell";
        double step = eq.equals("machine") ? 5.0 : 2.5;
        if (weight <= 0) return step; // never recommend 0 or negative
        return Math.round(weight / step) * step;
    }

    /** Minimum sensible weight for a given equipment type (never drop below this). */
    private double minimumWeight(String equipment) {
        String eq = equipment != null ? equipment.toLowerCase().trim() : "barbell";
        switch (eq) {
            case "machine":   return 10.0;
            case "dumbbell":  return  2.5;
            case "bodyweight": return 0.0;
            default:           return  2.5;
        }
    }

    // ...existing code...

    private void updateUserProfileWithSet(Long exerciseSessionId, Double weight, Integer reps) {
        try {
            ExerciseSessionEntity exerciseSession = exerciseSessionRepo.findById(exerciseSessionId)
                    .orElse(null);
            if (exerciseSession == null) return;

            WorkoutSessionEntity workoutSession = workoutSessionRepo.findById(exerciseSession.getWorkoutSessionId())
                    .orElse(null);
            if (workoutSession == null) return;

            UserProfileEntity profile = userProfileRepo.findByUserId(workoutSession.getUserId())
                    .orElse(null);
            if (profile != null) {
                Double weightLifted = weight * reps;
                profile.setTotalWeightLifted(profile.getTotalWeightLifted() + weightLifted);
                profile.setLastWorkoutDate(LocalDateTime.now());
                profile.setLastUpdatedAt(LocalDateTime.now());
                userProfileRepo.save(profile);
            }
        } catch (Exception e) {
            System.err.println("Error updating user profile with set data: " + e.getMessage());
        }
    }

    /**
     * Legacy method for backwards compatibility - just saves without evaluation.
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