package com.arcus.arc1.levelDeterminer;

/**
 * Constants for weight assignment based on user level and performance.
 * These provide baseline weights for new users and adjustment factors.
 */
public class WeightAssignmentConstants {

    // Base weights for new users by level (in kg/lbs)
    public static class BaseWeights {
        // Beginner level - conservative starting weights
        public static final Double BEGINNER_DUMBBELL = 5.0;      // 5 kg
        public static final Double BEGINNER_BARBELL = 20.0;      // 20 kg (bar only)
        public static final Double BEGINNER_COMPOUND = 30.0;     // 30 kg total
        public static final Double BEGINNER_ISOLATION = 15.0;    // 15 kg

        // Medium level
        public static final Double MEDIUM_DUMBBELL = 10.0;       // 10 kg
        public static final Double MEDIUM_BARBELL = 40.0;        // 40 kg
        public static final Double MEDIUM_COMPOUND = 60.0;       // 60 kg
        public static final Double MEDIUM_ISOLATION = 30.0;      // 30 kg

        // Advanced level
        public static final Double ADVANCED_DUMBBELL = 20.0;     // 20 kg
        public static final Double ADVANCED_BARBELL = 80.0;      // 80 kg
        public static final Double ADVANCED_COMPOUND = 100.0;    // 100 kg
        public static final Double ADVANCED_ISOLATION = 50.0;    // 50 kg

        // Expert level
        public static final Double EXPERT_DUMBBELL = 30.0;       // 30+ kg
        public static final Double EXPERT_BARBELL = 120.0;       // 120+ kg
        public static final Double EXPERT_COMPOUND = 150.0;      // 150+ kg
        public static final Double EXPERT_ISOLATION = 80.0;      // 80+ kg
    }

    // Weight adjustment factors based on rep performance
    public static class AdjustmentFactors {
        // If user completed all reps easily (reps > repMax)
        public static final Double INCREASE_SIGNIFICANT = 1.15;  // +15% weight

        // If user completed reps but near limit (reps >= repMin && reps <= repMax)
        public static final Double INCREASE_MODERATE = 1.10;     // +10% weight

        // If user struggled (reps < repMin)
        public static final Double DECREASE_MODERATE = 0.90;     // -10% weight

        // If user struggled significantly
        public static final Double DECREASE_SIGNIFICANT = 0.80;  // -20% weight

        // Minimum weight floor to prevent too-light assignments
        public static final Double MINIMUM_WEIGHT = 2.5;         // Don't go below 2.5 kg

        // Rounding increment - flexible progression
        public static final Double ROUNDING_INCREMENT = 2.5;     // Round to nearest 2.5 kg
    }

    // Rep range thresholds for adjustments
    public static class RepThresholds {
        // If average reps exceed repMax by this amount, increase weight
        public static final Integer INCREASE_REP_THRESHOLD = 2;  // If avg > repMax + 2

        // If average reps below repMin by this amount, decrease weight
        public static final Integer DECREASE_REP_THRESHOLD = 2;  // If avg < repMin - 2
    }

    // Set and rep range adjustment rules based on performance ratio (averageReps / repMax)
    public static class VolumeAdjustment {
        // Performance ratio thresholds
        // Ratio > 1.15 → user crushed it significantly (e.g. target 10, did 12+)
        public static final double CRUSHED_THRESHOLD = 1.15;
        // Ratio 1.0–1.15 → user hit or slightly exceeded target
        public static final double COMPLETED_THRESHOLD = 1.0;
        // Ratio 0.85–1.0 → user was within range but below max
        public static final double IN_RANGE_THRESHOLD = 0.85;
        // Ratio < 0.85 → user struggled

        // Set adjustments
        // When user crushes it for multiple sessions, reward with an extra set
        public static final int MAX_EXTRA_SETS = 1;        // Max sets above template default
        public static final int MAX_REMOVED_SETS = 1;      // Max sets below template default

        // Rep range shift (in reps) when weight changes significantly
        // When weight increases a lot, compress the rep range down
        public static final int REP_COMPRESS_AMOUNT = 2;   // Shift both repMin and repMax down by this
        // When weight decreases, expand the rep range up
        public static final int REP_EXPAND_AMOUNT = 2;     // Shift both repMin and repMax up by this

        // Hard limits for rep ranges to keep training sensible
        public static final int REP_MIN_FLOOR = 4;         // Never go below 4 reps (strength territory)
        public static final int REP_MAX_CEILING = 20;      // Never go above 20 reps (endurance territory)
    }
}

