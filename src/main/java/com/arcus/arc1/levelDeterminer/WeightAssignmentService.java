package com.arcus.arc1.levelDeterminer;

import com.arcus.arc1.ExerciseSession.ExerciseSessionEntity;
import com.arcus.arc1.ExerciseSession.ExerciseSessionRepo;
import com.arcus.arc1.SetLog.SetLogEntity;
import com.arcus.arc1.SetLog.SetLogRepo;
import com.arcus.arc1.WorkoutSession.WorkoutSessionEntity;
import com.arcus.arc1.WorkoutSession.WorkoutSessionRepo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
     * Assigns weight for a new user based on their level.
     * New users get baseline weights appropriate for their fitness level.
     *
     * @param userLevel User's fitness level (beginner, medium, advanced, expert)
     * @param exerciseName Name of the exercise
     * @param isCompound Whether the exercise is compound (multi-joint)
     * @return Suggested starting weight
     */
    public Double assignBaseWeight(String userLevel, String exerciseName, boolean isCompound) {
        return switch (userLevel.toLowerCase()) {
            case "beginner" -> assignBeginnerWeight(exerciseName, isCompound);
            case "medium" -> assignMediumWeight(exerciseName, isCompound);
            case "advanced" -> assignAdvancedWeight(exerciseName, isCompound);
            case "expert" -> assignExpertWeight(exerciseName, isCompound);
            default -> WeightAssignmentConstants.BaseWeights.BEGINNER_COMPOUND;
        };
    }

    /**
     * Assigns weight dynamically based on previous performance.
     * Analyzes the last 3 sessions to determine if user should increase/maintain/decrease weight.
     *
     * @param userId User ID
     * @param exerciseName Name of the exercise
     * @param currentWeight Current weight assigned
     * @param repMin Minimum reps target
     * @param repMax Maximum reps target
     * @return Suggested weight for next session
     */
    public Double assignDynamicWeight(Long userId, String exerciseName, Double currentWeight,
                                       Integer repMin, Integer repMax) {
        // Get recent exercise sessions (last 3 times user did this exercise)
        List<ExerciseSessionEntity> recentSessions = exerciseSessionRepo
                .findRecentExercisesByUserAndName(userId, exerciseName)
                .stream()
                .limit(3)
                .toList();

        if (recentSessions.isEmpty()) {
            // No history - return current weight
            return currentWeight;
        }

        // Calculate average reps achieved across last 3 sessions
        double averageReps = calculateAverageRepsForSessions(recentSessions);

        // Determine weight adjustment based on performance
        return calculateAdjustedWeight(currentWeight, averageReps, repMin, repMax);
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
            List<SetLogEntity> setsLogged = setLogRepo.findByExerciseSessionIdOrderBySetNumberAsc(session.getId());

            for (SetLogEntity set : setsLogged) {
                totalReps += set.getReps();
                totalSets++;
            }
        }

        return totalSets > 0 ? totalReps / totalSets : 0;
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
        // User completed significantly more reps than target
        if (averageReps > repMax + WeightAssignmentConstants.RepThresholds.INCREASE_REP_THRESHOLD) {
            return increaseWeight(currentWeight, WeightAssignmentConstants.AdjustmentFactors.INCREASE_SIGNIFICANT);
        }

        // User completed within range or slightly above max
        if (averageReps >= repMin && averageReps <= repMax + 1) {
            // Good performance - slight increase
            return increaseWeight(currentWeight, WeightAssignmentConstants.AdjustmentFactors.INCREASE_MODERATE);
        }

        // User struggled (reps below minimum)
        if (averageReps < repMin - WeightAssignmentConstants.RepThresholds.DECREASE_REP_THRESHOLD) {
            return decreaseWeight(currentWeight, WeightAssignmentConstants.AdjustmentFactors.DECREASE_SIGNIFICANT);
        }

        // User struggled slightly
        if (averageReps < repMin) {
            return decreaseWeight(currentWeight, WeightAssignmentConstants.AdjustmentFactors.DECREASE_MODERATE);
        }

        // Performance within range - maintain weight
        return currentWeight;
    }

    /**
     * Increases weight with safety guardrails.
     *
     * @param currentWeight Current weight
     * @param factor Multiplication factor
     * @return Increased weight, rounded to nearest 2.5
     */
    private Double increaseWeight(Double currentWeight, Double factor) {
        double newWeight = currentWeight * factor;
        // Round to nearest 2.5 for practicality (gym plates come in 2.5 kg increments)
        return Math.round(newWeight / 2.5) * 2.5;
    }

    /**
     * Decreases weight with minimum floor.
     *
     * @param currentWeight Current weight
     * @param factor Multiplication factor (< 1.0)
     * @return Decreased weight, not below minimum
     */
    private Double decreaseWeight(Double currentWeight, Double factor) {
        double newWeight = currentWeight * factor;
        // Never go below minimum safe weight
        return Math.max(newWeight, WeightAssignmentConstants.AdjustmentFactors.MINIMUM_WEIGHT);
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


