package com.arcus.arc1.SetLog;

import com.arcus.arc1.dto.SetEvaluationDTO;
import com.arcus.arc1.ExerciseSession.ExerciseSessionEntity;
import com.arcus.arc1.ExerciseSession.ExerciseSessionRepo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for evaluating set performance.
 * Detects fatigue based on rep drops and suggests appropriate rest periods.
 */
@Service
public class SetEvaluationService {

    private final SetLogRepo setLogRepo;
    private final ExerciseSessionRepo exerciseSessionRepo;

    // Fatigue detection thresholds
    private static final Integer REP_DROP_THRESHOLD = 2; // Reps must drop by at least 2 to indicate fatigue
    private static final Double REP_DROP_PERCENTAGE = 0.15; // 15% rep drop indicates fatigue

    // Rest time recommendations (in seconds) based on rep range
    private static final Integer REST_HEAVY_COMPOUND = 180; // 3 minutes for heavy compound lifts
    private static final Integer REST_MODERATE = 120; // 2 minutes for moderate intensity
    private static final Integer REST_LIGHT = 60; // 1 minute for light isolation work

    public SetEvaluationService(SetLogRepo setLogRepo, ExerciseSessionRepo exerciseSessionRepo) {
        this.setLogRepo = setLogRepo;
        this.exerciseSessionRepo = exerciseSessionRepo;
    }

    /**
     * Evaluates a set that was just logged.
     *
     * @param exerciseSessionId The exercise session the set belongs to
     * @param setNumber The set number that was just logged
     * @return SetEvaluationDTO with fatigue detection and rest recommendations
     */
    public SetEvaluationDTO evaluateSet(Long exerciseSessionId, Integer setNumber) {
        // Fetch all sets for this exercise session
        List<SetLogEntity> allSets = new ArrayList<>();

        if (allSets.isEmpty()) {
            return new SetEvaluationDTO(
                    false,
                    REST_MODERATE,
                    "Set logged successfully.",
                    false
            );
        }

        // Find the current set and first set
        SetLogEntity currentSet = allSets.stream()
                .filter(s -> s.getSetNumber().equals(setNumber))
                .findFirst()
                .orElse(null);

        if (currentSet == null) {
            return new SetEvaluationDTO(
                    false,
                    REST_MODERATE,
                    "Set logged successfully.",
                    false
            );
        }

        SetLogEntity firstSet = allSets.getFirst();

        // Detect fatigue by comparing current set to first set
        boolean fatigue = detectFatigue(firstSet, currentSet);

        // Determine rest time based on rep range
        Integer restTime = determineRestTime(currentSet);

        // Generate appropriate message
        String message = generateMessage(fatigue, currentSet, firstSet);

        // Check if exercise is completed (all planned sets logged)
        boolean exerciseCompleted = isExerciseCompleted(exerciseSessionId, allSets.size());

        return new SetEvaluationDTO(fatigue, restTime, message, exerciseCompleted);
    }

    /**
     * Detects fatigue by comparing the current set to the first set.
     * Fatigue is detected if:
     * 1. Reps dropped by at least REP_DROP_THRESHOLD, OR
     * 2. Reps dropped by more than REP_DROP_PERCENTAGE percentage
     *
     * @param firstSet The first set of the exercise
     * @param currentSet The current set
     * @return true if fatigue is detected, false otherwise
     */
    private boolean detectFatigue(SetLogEntity firstSet, SetLogEntity currentSet) {
        int repDifference = firstSet.getReps() - currentSet.getReps();

        // Absolute rep drop threshold
        if (repDifference >= REP_DROP_THRESHOLD) {
            return true;
        }

        // Percentage-based rep drop
        if (firstSet.getReps() > 0) {
            double percentageDrop = (double) repDifference / firstSet.getReps();
            return percentageDrop >= REP_DROP_PERCENTAGE;
        }

        return false;
    }

    /**
     * Determines recommended rest time based on rep range.
     * Higher reps = lighter weight = shorter rest
     * Lower reps = heavier weight = longer rest
     *
     * @param set The set to evaluate
     * @return Rest time in seconds
     */
    private Integer determineRestTime(SetLogEntity set) {
        Integer reps = set.getReps();

        // Heavy compound (low reps, heavy weight)
        if (reps < 6) {
            return REST_HEAVY_COMPOUND;
        }

        // Moderate intensity
        if (reps < 12) {
            return REST_MODERATE;
        }

        // Light isolation (high reps, lighter weight)
        return REST_LIGHT;
    }

    /**
     * Generates a user-friendly message based on set performance.
     *
     * @param fatigueDetected Whether fatigue was detected
     * @param currentSet The current set
     * @param firstSet The first set
     * @return A descriptive message
     */
    private String generateMessage(boolean fatigueDetected, SetLogEntity currentSet, SetLogEntity firstSet) {
        if (!fatigueDetected) {
            return "Good control. Maintain tempo.";
        }

        int repDrop = firstSet.getReps() - currentSet.getReps();
        double percentageDrop = (double) repDrop / firstSet.getReps() * 100;

        return String.format(
                "Fatigue detected. Reps dropped %.0f%% from first set. Extend rest or reduce weight on next set.",
                percentageDrop
        );
    }

    /**
     * Checks if the exercise session has all planned sets completed.
     * Updates the exercise session's completedSets and isCompleted fields in the database.
     *
     * @param exerciseSessionId The exercise session ID
     * @param totalSetsLogged The total number of sets logged so far
     * @return true if total sets logged equals planned sets, false otherwise
     */
    private boolean isExerciseCompleted(Long exerciseSessionId, int totalSetsLogged) {
        ExerciseSessionEntity exercise = exerciseSessionRepo.findById(exerciseSessionId).orElse(null);

        if (exercise == null) {
            return false;
        }

        Integer plannedSets = exercise.getSets();

        // Update completedSets in the database
        exercise.setCompletedSets(totalSetsLogged);

        // Check if all sets are completed and update isCompleted flag
        boolean allSetsCompleted = plannedSets != null && plannedSets == totalSetsLogged;
        exercise.setIsCompleted(allSetsCompleted);

        // Save the updated exercise session to database
        exerciseSessionRepo.save(exercise);

        return allSetsCompleted;
    }
}


