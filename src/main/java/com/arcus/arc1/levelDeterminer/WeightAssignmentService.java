package com.arcus.arc1.levelDeterminer;

import com.arcus.arc1.ExerciseSession.ExerciseSessionEntity;
import com.arcus.arc1.ExerciseSession.ExerciseSessionRepo;
import com.arcus.arc1.SetLog.SetLogEntity;
import com.arcus.arc1.SetLog.SetLogRepo;
import com.arcus.arc1.WorkoutSession.WorkoutSessionEntity;
import com.arcus.arc1.WorkoutSession.WorkoutSessionRepo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for intelligent dynamic weight assignment.
 *
 * Assigns weights based on:
 * 1. User level (beginner, medium, advanced, expert)
 * 2. Previous performance (rep logs from last 3 sessions)
 * 3. Rep range targets (repMin, repMax)
 *
 * This creates a progressive overload system that adapts to user performance.
 */
@Service
public class WeightAssignmentService {

    private final WorkoutSessionRepo workoutSessionRepo;
    private final SetLogRepo setLogRepo;
    private final ExerciseSessionRepo exerciseSessionRepo;

    public WeightAssignmentService(WorkoutSessionRepo workoutSessionRepo,
                                   SetLogRepo setLogRepo,
                                   ExerciseSessionRepo exerciseSessionRepo) {
        this.workoutSessionRepo = workoutSessionRepo;
        this.setLogRepo = setLogRepo;
        this.exerciseSessionRepo = exerciseSessionRepo;
    }

    /**
     * Bundles all dynamic adjustments for an exercise into one result.
     *
     * @param weight      Adjusted target weight
     * @param sets        Adjusted number of sets
     * @param repMin      Adjusted minimum reps
     * @param repMax      Adjusted maximum reps
     */
    public record WorkoutAdjustment(Double weight, Integer sets, Integer repMin, Integer repMax) {}

    /**
     * Assigns weight for a new user based on their level.
     * New users get baseline weights appropriate for their fitness level.
     *
     * @param userLevel User's fitness level (beginner, medium, advanced, expert)
     * @param exerciseName Name of the exercise
     * @param isCompound Whether the exercise is compound (multi-joint)
     * @return Suggested starting weight
     */
    public Double assignBaseWeight(String userLevel, String exerciseName, boolean isCompound) {
        Double raw = switch (userLevel.toLowerCase()) {
            case "beginner" -> assignBeginnerWeight(exerciseName, isCompound);
            case "medium" -> assignMediumWeight(exerciseName, isCompound);
            case "advanced" -> assignAdvancedWeight(exerciseName, isCompound);
            case "expert" -> assignExpertWeight(exerciseName, isCompound);
            default -> WeightAssignmentConstants.BaseWeights.BEGINNER_COMPOUND;
        };
        return roundToGymWeight(raw);
    }

    /**
     * Returns a full dynamic adjustment — weight, sets, repMin, repMax — based on
     * the user's recent performance relative to the template defaults.
     *
     * Logic (driven by performance ratio = averageReps / repMax):
     *
     *  ratio > 1.15  (crushed it)     → weight +15%, rep range shifts DOWN, sets +1 if sustained
     *  ratio 1.0–1.15 (hit target)    → weight +10%, rep range unchanged, sets unchanged
     *  ratio 0.85–1.0 (within range)  → weight +10%, rep range unchanged, sets unchanged
     *  ratio < 0.85  (struggled)      → weight −10/20%, rep range shifts UP, sets −1
     *
     * @param recentSessions    Pre-fetched recent exercise sessions (with set logs)
     * @param currentWeight     Last session's target weight
     * @param templateSets      Template-defined default number of sets
     * @param templateRepMin    Template-defined minimum reps
     * @param templateRepMax    Template-defined maximum reps
     * @return WorkoutAdjustment with new weight, sets, repMin, repMax
     */
    public WorkoutAdjustment assignDynamicAdjustment(List<ExerciseSessionEntity> recentSessions,
                                                      Double currentWeight, Integer templateSets,
                                                      Integer templateRepMin, Integer templateRepMax) {
        if (recentSessions.isEmpty()) {
            return new WorkoutAdjustment(currentWeight, templateSets, templateRepMin, templateRepMax);
        }

        double averageReps = calculateAverageRepsForSessions(recentSessions);

        // No set logs found in history (sessions exist but were never logged) — keep current weight
        if (averageReps == 0) {
            return new WorkoutAdjustment(currentWeight, templateSets, templateRepMin, templateRepMax);
        }

        double performanceRatio = averageReps / templateRepMax;

        Double newWeight;
        int newSets = templateSets;
        int newRepMin = templateRepMin;
        int newRepMax = templateRepMax;

        if (performanceRatio > WeightAssignmentConstants.VolumeAdjustment.CRUSHED_THRESHOLD) {
            // User is comfortably exceeding the target — increase weight and compress rep range
            // so they're still working hard at the new heavier load
            newWeight = increaseWeight(currentWeight, WeightAssignmentConstants.AdjustmentFactors.INCREASE_SIGNIFICANT);
            newRepMin = Math.max(
                    templateRepMin - WeightAssignmentConstants.VolumeAdjustment.REP_COMPRESS_AMOUNT,
                    WeightAssignmentConstants.VolumeAdjustment.REP_MIN_FLOOR
            );
            newRepMax = Math.max(
                    templateRepMax - WeightAssignmentConstants.VolumeAdjustment.REP_COMPRESS_AMOUNT,
                    WeightAssignmentConstants.VolumeAdjustment.REP_MIN_FLOOR + 1
            );
            // Reward sustained dominance with an extra set
            newSets = Math.min(
                    templateSets + WeightAssignmentConstants.VolumeAdjustment.MAX_EXTRA_SETS,
                    templateSets + 1
            );

        } else if (performanceRatio >= WeightAssignmentConstants.VolumeAdjustment.COMPLETED_THRESHOLD) {
            // User hit or slightly exceeded target — standard progression, rep range stays
            newWeight = increaseWeight(currentWeight, WeightAssignmentConstants.AdjustmentFactors.INCREASE_MODERATE);
            // Rep range and sets stay as template

        } else if (performanceRatio >= WeightAssignmentConstants.VolumeAdjustment.IN_RANGE_THRESHOLD) {
            // User was in range but below max — still increase weight, rep range stays
            newWeight = increaseWeight(currentWeight, WeightAssignmentConstants.AdjustmentFactors.INCREASE_MODERATE);
            // Rep range and sets stay as template

        } else {
            // User struggled — reduce weight and expand rep range upward so volume is maintained
            // at a lighter load with more reps
            boolean significantStruggle = averageReps < templateRepMin -
                    WeightAssignmentConstants.RepThresholds.DECREASE_REP_THRESHOLD;
            newWeight = significantStruggle
                    ? decreaseWeight(currentWeight, WeightAssignmentConstants.AdjustmentFactors.DECREASE_SIGNIFICANT)
                    : decreaseWeight(currentWeight, WeightAssignmentConstants.AdjustmentFactors.DECREASE_MODERATE);

            newRepMin = Math.min(
                    templateRepMin + WeightAssignmentConstants.VolumeAdjustment.REP_EXPAND_AMOUNT,
                    WeightAssignmentConstants.VolumeAdjustment.REP_MAX_CEILING - 1
            );
            newRepMax = Math.min(
                    templateRepMax + WeightAssignmentConstants.VolumeAdjustment.REP_EXPAND_AMOUNT,
                    WeightAssignmentConstants.VolumeAdjustment.REP_MAX_CEILING
            );
            // Reduce sets by 1 to prevent overtraining when struggling
            newSets = Math.max(
                    templateSets - WeightAssignmentConstants.VolumeAdjustment.MAX_REMOVED_SETS,
                    1
            );
        }

        return new WorkoutAdjustment(newWeight, newSets, newRepMin, newRepMax);
    }

    /**
     * Calculates average reps achieved for specific exercise sessions.
     *
     * @param exerciseSessions List of recent exercise sessions
     * @return Average reps achieved across all sets
     */
    private double calculateAverageRepsForSessions(List<ExerciseSessionEntity> exerciseSessions) {
        double totalReps = 0;
        int totalSets = 0;

        for (ExerciseSessionEntity session : exerciseSessions) {
            // Get all set logs for this exercise session
            List<SetLogEntity> setsLogged = new ArrayList<>();

            System.out.println("[WEIGHT-DEBUG] Session id=" + session.getId() +
                    " ('" + session.getExerciseName() + "') → set logs found: " + setsLogged.size());

            for (SetLogEntity set : setsLogged) {
                System.out.println("[WEIGHT-DEBUG]   Set #" + set.getSetNumber() +
                        " → weight=" + set.getWeight() + ", reps=" + set.getReps());
                totalReps += set.getReps();
                totalSets++;
            }
        }

        double avg = totalSets > 0 ? totalReps / totalSets : 0;
        System.out.println("[WEIGHT-DEBUG] averageReps = " + avg + " (totalReps=" + totalReps + ", totalSets=" + totalSets + ")");
        return avg;
    }

    /**
     * Calculates adjusted weight based on average reps vs target range.
     *
     * @param currentWeight Current weight
     * @param averageReps Average reps achieved
     * @param repMin Minimum target reps
     * @param repMax Maximum target reps
     * @return Adjusted weight
     */
    private Double calculateAdjustedWeight(Double currentWeight, double averageReps,
                                           Integer repMin, Integer repMax) {
        // User completed significantly more reps than target - increase weight significantly
        if (averageReps > repMax + WeightAssignmentConstants.RepThresholds.INCREASE_REP_THRESHOLD) {
            return increaseWeight(currentWeight, WeightAssignmentConstants.AdjustmentFactors.INCREASE_SIGNIFICANT);
        }

        // User completed within or above target range - increase weight moderately
        if (averageReps >= repMax) {
            return increaseWeight(currentWeight, WeightAssignmentConstants.AdjustmentFactors.INCREASE_MODERATE);
        }

        // User completed all sets within the range but not at max - still increase
        if (averageReps >= repMin) {
            return increaseWeight(currentWeight, WeightAssignmentConstants.AdjustmentFactors.INCREASE_MODERATE);
        }

        // User struggled significantly (reps well below minimum)
        if (averageReps < repMin - WeightAssignmentConstants.RepThresholds.DECREASE_REP_THRESHOLD) {
            return decreaseWeight(currentWeight, WeightAssignmentConstants.AdjustmentFactors.DECREASE_SIGNIFICANT);
        }

        // User struggled slightly (reps below minimum but not by much)
        if (averageReps < repMin) {
            return decreaseWeight(currentWeight, WeightAssignmentConstants.AdjustmentFactors.DECREASE_MODERATE);
        }

        // Should not reach here but maintain weight as fallback
        return currentWeight;
    }

    /**
     * Increases weight with safety guardrails.
     *
     * @param currentWeight Current weight
     * @param factor Multiplication factor
     * @return Increased weight, rounded to nearest 5kg
     */
    private Double increaseWeight(Double currentWeight, Double factor) {
        return roundToGymWeight(currentWeight * factor);
    }

    private Double decreaseWeight(Double currentWeight, Double factor) {
        double newWeight = currentWeight * factor;
        return Math.max(roundToGymWeight(newWeight), WeightAssignmentConstants.AdjustmentFactors.MINIMUM_WEIGHT);
    }

    /**
     * Rounds a weight to the nearest valid gym increment.
     * Valid weights are multiples of 5, or multiples of 5 ± 2.5 (e.g. 2.5, 5, 7.5, 10, 12.5, ...)
     * This is achieved by rounding to the nearest 2.5.
     */
    private Double roundToGymWeight(double weight) {
        final double increment = WeightAssignmentConstants.AdjustmentFactors.ROUNDING_INCREMENT; // 2.5
        return Math.round(weight / increment) * increment;
    }

    // Weight assignment by level
    private Double assignBeginnerWeight(String exerciseName, boolean isCompound) {
        if (exerciseName.toLowerCase().contains("barbell")) {
            return WeightAssignmentConstants.BaseWeights.BEGINNER_BARBELL;
        } else if (exerciseName.toLowerCase().contains("dumbbell")) {
            return WeightAssignmentConstants.BaseWeights.BEGINNER_DUMBBELL;
        }
        return isCompound ? WeightAssignmentConstants.BaseWeights.BEGINNER_COMPOUND :
                WeightAssignmentConstants.BaseWeights.BEGINNER_ISOLATION;
    }

    private Double assignMediumWeight(String exerciseName, boolean isCompound) {
        if (exerciseName.toLowerCase().contains("barbell")) {
            return WeightAssignmentConstants.BaseWeights.MEDIUM_BARBELL;
        } else if (exerciseName.toLowerCase().contains("dumbbell")) {
            return WeightAssignmentConstants.BaseWeights.MEDIUM_DUMBBELL;
        }
        return isCompound ? WeightAssignmentConstants.BaseWeights.MEDIUM_COMPOUND :
                WeightAssignmentConstants.BaseWeights.MEDIUM_ISOLATION;
    }

    private Double assignAdvancedWeight(String exerciseName, boolean isCompound) {
        if (exerciseName.toLowerCase().contains("barbell")) {
            return WeightAssignmentConstants.BaseWeights.ADVANCED_BARBELL;
        } else if (exerciseName.toLowerCase().contains("dumbbell")) {
            return WeightAssignmentConstants.BaseWeights.ADVANCED_DUMBBELL;
        }
        return isCompound ? WeightAssignmentConstants.BaseWeights.ADVANCED_COMPOUND :
                WeightAssignmentConstants.BaseWeights.ADVANCED_ISOLATION;
    }

    private Double assignExpertWeight(String exerciseName, boolean isCompound) {
        if (exerciseName.toLowerCase().contains("barbell")) {
            return WeightAssignmentConstants.BaseWeights.EXPERT_BARBELL;
        } else if (exerciseName.toLowerCase().contains("dumbbell")) {
            return WeightAssignmentConstants.BaseWeights.EXPERT_DUMBBELL;
        }
        return isCompound ? WeightAssignmentConstants.BaseWeights.EXPERT_COMPOUND :
                WeightAssignmentConstants.BaseWeights.EXPERT_ISOLATION;
    }
}


