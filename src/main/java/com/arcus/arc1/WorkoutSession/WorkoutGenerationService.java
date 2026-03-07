package com.arcus.arc1.WorkoutSession;

import com.arcus.arc1.ExerciseSession.ExerciseSessionEntity;
import com.arcus.arc1.ExerciseSession.ExerciseSessionRepo;
import com.arcus.arc1.TemplateExcercise.TemplateExerciseEntity;
import com.arcus.arc1.TemplateExcercise.TemplateExerciseRepo;
import com.arcus.arc1.WorkoutTemplate.WorkoutTemplateEntity;
import com.arcus.arc1.WorkoutTemplate.WorkoutTemplateRepo;
import com.arcus.arc1.UserProfile.UserProfileEntity;
import com.arcus.arc1.UserProfile.UserProfileRepo;
import com.arcus.arc1.dto.ExerciseDTO;
import com.arcus.arc1.dto.WorkoutResponseDTO;
import com.arcus.arc1.levelDeterminer.WeightAssignmentService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for generating workouts with intelligent weight assignment.
 *
 * Workflow:
 * 1. Fetch workout template based on user level
 * 2. Create workout session
 * 3. For each exercise in template:
 *    - Assign weight based on user level (new users) or previous performance
 *    - Create exercise session with assigned weight
 * 4. Return workout with dynamically assigned weights
 */
@Service
public class WorkoutGenerationService {

    private final WorkoutTemplateRepo templateRepo;
    private final TemplateExerciseRepo exerciseRepo;
    private final WorkoutSessionRepo workoutSessionRepo;
    private final ExerciseSessionRepo exerciseSessionRepo;
    private final WeightAssignmentService weightAssignmentService;
    private final UserProfileRepo userProfileRepo;

    public WorkoutGenerationService(
            WorkoutTemplateRepo templateRepo,
            TemplateExerciseRepo exerciseRepo,
            WorkoutSessionRepo workoutSessionRepo,
            ExerciseSessionRepo exerciseSessionRepo,
            WeightAssignmentService weightAssignmentService,
            UserProfileRepo userProfileRepo
    ) {
        this.templateRepo = templateRepo;
        this.exerciseRepo = exerciseRepo;
        this.workoutSessionRepo = workoutSessionRepo;
        this.exerciseSessionRepo = exerciseSessionRepo;
        this.weightAssignmentService = weightAssignmentService;
        this.userProfileRepo = userProfileRepo;
    }

    /**
     * Generates a new workout for a user with intelligently assigned weights.
     * Also updates the user's profile with workout statistics.
     *
     * @param userId User ID
     * @param level User's fitness level
     * @return WorkoutResponseDTO with exercises and assigned weights
     */
    public WorkoutResponseDTO generateWorkout(Long userId, String level) {

        List<ExerciseDTO> exerciseDTOList = new ArrayList<>();

        // Fetch template for this level
        WorkoutTemplateEntity template =
                templateRepo.findByLevelAndGoalAndDayNumber(
                        level,
                        "muscle_gain",
                        1 // for now hardcoded day 1
                ).orElseThrow();

        // Create new workout session
        WorkoutSessionEntity session = new WorkoutSessionEntity();
        session.setUserId(userId);
        session.setTemplateId(template.getId());
        session.setCreatedAt(LocalDateTime.now());
        session.setCompleted(false);

        session = workoutSessionRepo.save(session);

        // Fetch exercises from template
        List<TemplateExerciseEntity> exercises =
                exerciseRepo.findByTemplateIdOrderByOrderIndex(template.getId());

        // Create exercise sessions with intelligent weight assignment
        int totalSets = 0;
        double totalWeightCalculated = 0.0;

        for (TemplateExerciseEntity te : exercises) {

            ExerciseSessionEntity es = new ExerciseSessionEntity();
            es.setWorkoutSessionId(session.getId());
            es.setExerciseName(te.getExerciseName());
            es.setRepMin(te.getRepMin());
            es.setRepMax(te.getRepMax());
            es.setSets(te.getSets());
            es.setTempo(te.getTempo());

            // Intelligently assign weight
            Double assignedWeight = assignWeightForExercise(userId, level, te);
            es.setTargetWeight(assignedWeight);

            es = exerciseSessionRepo.save(es);

            // Calculate total sets and weight for profile update
            totalSets += te.getSets();
            // Weight = weight × average reps (using rep max as estimate)
            totalWeightCalculated += assignedWeight * te.getRepMax();

            // Build response DTO
            ExerciseDTO exerciseDTO = new ExerciseDTO();
            exerciseDTO.setExerciseSessionId(es.getId());
            exerciseDTO.setExerciseName(es.getExerciseName());
            exerciseDTO.setTargetWeight(es.getTargetWeight());
            exerciseDTO.setRepMin(es.getRepMin());
            exerciseDTO.setRepMax(es.getRepMax());
            exerciseDTO.setSets(es.getSets());
            exerciseDTO.setTempo(es.getTempo());

            exerciseDTOList.add(exerciseDTO);
        }

        // Update user profile with workout statistics
        updateUserProfile(userId, exercises.size(), totalSets, totalWeightCalculated);

        return new WorkoutResponseDTO(session.getId(), level, exerciseDTOList);
    }

    /**
     * Updates user profile with workout statistics after generating a workout.
     *
     * @param userId User ID
     * @param totalExercises Number of exercises in the workout
     * @param totalSets Total number of sets in the workout
     * @param totalWeight Estimated total weight to be lifted
     */
    private void updateUserProfile(Long userId, int totalExercises, int totalSets, double totalWeight) {
        try {
            UserProfileEntity profile = userProfileRepo.findByUserId(userId)
                    .orElse(null);

            if (profile != null) {
                // Update workout statistics
                profile.setTotalWorkouts(profile.getTotalWorkouts() + 1);
                profile.setTotalExerciseSessions(profile.getTotalExerciseSessions() + totalExercises);
                profile.setTotalSetsSessions(profile.getTotalSetsSessions() + totalSets);
                profile.setTotalWeightLifted(profile.getTotalWeightLifted() + totalWeight);
                profile.setLastWorkoutDate(LocalDateTime.now());
                profile.setLastUpdatedAt(LocalDateTime.now());

                // Track consecutive workout days (simplified - can be enhanced)
                profile.setConsecutiveWorkoutDays(profile.getConsecutiveWorkoutDays() + 1);

                userProfileRepo.save(profile);
            }
        } catch (Exception e) {
            // Log error but don't fail workout generation if profile update fails
            System.err.println("Error updating user profile: " + e.getMessage());
        }
    }

    /**
     * Assigns weight for an exercise based on user history and level.
     *
     * @param userId User ID
     * @param level User's level
     * @param templateExercise Template exercise with rep ranges
     * @return Assigned weight
     */
    private Double assignWeightForExercise(Long userId, String level, TemplateExerciseEntity templateExercise) {
        String exerciseName = templateExercise.getExerciseName();

        // Check if user has previous performance data for this exercise
        List<ExerciseSessionEntity> previousSessions = exerciseSessionRepo
                .findRecentExercisesByUserAndName(userId, exerciseName)
                .stream()
                .limit(3)
                .toList();

        if (previousSessions.isEmpty()) {
            // New user or first time doing this exercise - assign base weight by level
            boolean isCompound = isCompoundExercise(exerciseName);
            return weightAssignmentService.assignBaseWeight(level, exerciseName, isCompound);
        }

        // User has history - assign dynamically based on previous performance
        // Use the most recent weight as baseline
        Double currentWeight = previousSessions.getFirst().getTargetWeight();
        return weightAssignmentService.assignDynamicWeight(
                userId,
                exerciseName,
                currentWeight,
                templateExercise.getRepMin(),
                templateExercise.getRepMax()
        );
    }

    /**
     * Determines if an exercise is compound (multi-joint) or isolation.
     * Compound exercises typically involve larger muscle groups and use more weight.
     *
     * @param exerciseName Name of the exercise
     * @return true if compound, false if isolation
     */
    private boolean isCompoundExercise(String exerciseName) {
        String name = exerciseName.toLowerCase();

        // Compound exercises
        String[] compoundExercises = {
                "squat", "deadlift", "bench press", "row", "press",
                "pull", "dip", "clean", "snatch", "overhead"
        };

        for (String compound : compoundExercises) {
            if (name.contains(compound)) {
                return true;
            }
        }

        return false;
    }
}

