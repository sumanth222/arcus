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
        public static final Double INCREASE_SIGNIFICANT = 1.10;  // +10% weight

        // If user completed reps but near limit (reps >= repMin && reps <= repMax)
        public static final Double INCREASE_MODERATE = 1.05;     // +5% weight

        // If user struggled (reps < repMin)
        public static final Double DECREASE_MODERATE = 0.95;     // -5% weight

        // If user struggled significantly
        public static final Double DECREASE_SIGNIFICANT = 0.90;  // -10% weight

        // Minimum weight floor to prevent too-light assignments
        public static final Double MINIMUM_WEIGHT = 2.5;         // Don't go below 2.5 kg
    }

    // Rep range thresholds for adjustments
    public static class RepThresholds {
        // If average reps exceed repMax by this amount, increase weight
        public static final Integer INCREASE_REP_THRESHOLD = 2;  // If avg > repMax + 2

        // If average reps below repMin by this amount, decrease weight
        public static final Integer DECREASE_REP_THRESHOLD = 2;  // If avg < repMin - 2
    }
}

