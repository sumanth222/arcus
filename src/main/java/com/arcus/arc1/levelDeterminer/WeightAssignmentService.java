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
    /**
     * Returns true if the exercise weight is split between two hands/sides (dumbbell, barbell, bench).
     * The UI divides the returned total weight by 2 for display, so the total must be a multiple
     * of 5 to ensure each hand holds a valid dumbbell weight (multiple of 2.5).
     */
    public static boolean isSplitWeightExercise(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.contains("dumbbell") || lower.contains("db ")
                || lower.contains(" db") || lower.contains("barbell")
                || lower.contains("bb ") || lower.contains(" bb")
                || lower.contains("bench");
    }

    /**
     * Rounds a weight to the nearest valid gym increment, respecting whether the
     * weight is split between two hands.
     *
     * Split exercises (dumbbell / barbell / bench):
     *   → total must be a multiple of 5 so per-hand = total/2 is a valid 2.5-multiple
     * Other exercises (cables, machines, bodyweight):
     *   → nearest 2.5 is fine
     */
    public Double roundToGymWeight(double weight, String exerciseName) {
        if (isSplitWeightExercise(exerciseName)) {
            // Round to nearest 5; minimum 5 so each hand is at least 2.5 kg
            return Math.max(5.0, Math.round(weight / 5.0) * 5.0);
        }
        return roundToGymWeight(weight);
    }

    public Double assignBaseWeight(String userLevel, String exerciseName, boolean isCompound) {
        Double raw = switch (userLevel.toLowerCase()) {
            case "beginner" -> assignBeginnerWeight(exerciseName, isCompound);
            case "medium" -> assignMediumWeight(exerciseName, isCompound);
            case "advanced" -> assignAdvancedWeight(exerciseName, isCompound);
            case "expert" -> assignExpertWeight(exerciseName, isCompound);
            default -> WeightAssignmentConstants.BaseWeights.BEGINNER_COMPOUND;
        };
        return roundToGymWeight(raw, exerciseName);  // use split-aware rounding
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
                                                      Integer templateRepMin, Integer templateRepMax,
                                                      String exerciseName) {
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
            newWeight = increaseWeight(currentWeight, WeightAssignmentConstants.AdjustmentFactors.INCREASE_SIGNIFICANT, exerciseName);
            newRepMin = Math.max(
                    templateRepMin - WeightAssignmentConstants.VolumeAdjustment.REP_COMPRESS_AMOUNT,
                    WeightAssignmentConstants.VolumeAdjustment.REP_MIN_FLOOR
            );
            newRepMax = Math.max(
                    templateRepMax - WeightAssignmentConstants.VolumeAdjustment.REP_COMPRESS_AMOUNT,
                    WeightAssignmentConstants.VolumeAdjustment.REP_MIN_FLOOR + 1
            );
            newSets = Math.min(
                    templateSets + WeightAssignmentConstants.VolumeAdjustment.MAX_EXTRA_SETS,
                    templateSets + 1
            );
        } else if (performanceRatio >= WeightAssignmentConstants.VolumeAdjustment.COMPLETED_THRESHOLD) {
            newWeight = increaseWeight(currentWeight, WeightAssignmentConstants.AdjustmentFactors.INCREASE_MODERATE, exerciseName);
        } else if (performanceRatio >= WeightAssignmentConstants.VolumeAdjustment.IN_RANGE_THRESHOLD) {
            newWeight = increaseWeight(currentWeight, WeightAssignmentConstants.AdjustmentFactors.INCREASE_MODERATE, exerciseName);
        } else {
            boolean significantStruggle = averageReps < templateRepMin -
                    WeightAssignmentConstants.RepThresholds.DECREASE_REP_THRESHOLD;
            newWeight = significantStruggle
                    ? decreaseWeight(currentWeight, WeightAssignmentConstants.AdjustmentFactors.DECREASE_SIGNIFICANT, exerciseName)
                    : decreaseWeight(currentWeight, WeightAssignmentConstants.AdjustmentFactors.DECREASE_MODERATE, exerciseName);
            newRepMin = Math.min(
                    templateRepMin + WeightAssignmentConstants.VolumeAdjustment.REP_EXPAND_AMOUNT,
                    WeightAssignmentConstants.VolumeAdjustment.REP_MAX_CEILING - 1
            );
            newRepMax = Math.min(
                    templateRepMax + WeightAssignmentConstants.VolumeAdjustment.REP_EXPAND_AMOUNT,
                    WeightAssignmentConstants.VolumeAdjustment.REP_MAX_CEILING
            );
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
                                           Integer repMin, Integer repMax, String exerciseName) {
        if (averageReps > repMax + WeightAssignmentConstants.RepThresholds.INCREASE_REP_THRESHOLD) {
            return increaseWeight(currentWeight, WeightAssignmentConstants.AdjustmentFactors.INCREASE_SIGNIFICANT, exerciseName);
        }
        if (averageReps >= repMax) {
            return increaseWeight(currentWeight, WeightAssignmentConstants.AdjustmentFactors.INCREASE_MODERATE, exerciseName);
        }
        if (averageReps >= repMin) {
            return increaseWeight(currentWeight, WeightAssignmentConstants.AdjustmentFactors.INCREASE_MODERATE, exerciseName);
        }
        if (averageReps < repMin - WeightAssignmentConstants.RepThresholds.DECREASE_REP_THRESHOLD) {
            return decreaseWeight(currentWeight, WeightAssignmentConstants.AdjustmentFactors.DECREASE_SIGNIFICANT, exerciseName);
        }
        if (averageReps < repMin) {
            return decreaseWeight(currentWeight, WeightAssignmentConstants.AdjustmentFactors.DECREASE_MODERATE, exerciseName);
        }
        return currentWeight;
    }

    /**
     * Increases weight with safety guardrails.
     *
     * @param currentWeight Current weight
     * @param factor Multiplication factor
     * @return Increased weight, rounded to nearest 5kg
     */
    private Double increaseWeight(Double currentWeight, Double factor, String exerciseName) {
        return roundToGymWeight(currentWeight * factor, exerciseName);
    }

    private Double decreaseWeight(Double currentWeight, Double factor, String exerciseName) {
        double newWeight = currentWeight * factor;
        return Math.max(roundToGymWeight(newWeight, exerciseName), WeightAssignmentConstants.AdjustmentFactors.MINIMUM_WEIGHT);
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


