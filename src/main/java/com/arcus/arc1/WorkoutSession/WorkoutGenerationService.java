package com.arcus.arc1.WorkoutSession;

import com.arcus.arc1.ExerciseSession.ExerciseSessionEntity;
import com.arcus.arc1.ExerciseSession.ExerciseSessionRepo;
import com.arcus.arc1.TemplateExcercise.TemplateExerciseEntity;
import com.arcus.arc1.TemplateExcercise.TemplateExerciseRepo;
import com.arcus.arc1.WorkoutExerciseTemplateRepository.WorkoutExerciseTemplateEntity;
import com.arcus.arc1.WorkoutExerciseTemplateRepository.WorkoutExerciseTemplateRepository;
import com.arcus.arc1.WorkoutTemplate.WorkoutTemplateEntity;
import com.arcus.arc1.WorkoutTemplate.WorkoutTemplateRepo;
import com.arcus.arc1.UserProfile.UserProfileEntity;
import com.arcus.arc1.UserProfile.UserProfileRepo;
import com.arcus.arc1.ExerciseLibrary.ExerciseLibraryRepo;
import com.arcus.arc1.ExerciseLibrary.ExerciseLibraryEntity;
import com.arcus.arc1.dto.ExerciseDTO;
import com.arcus.arc1.dto.MuscleRequest;
import com.arcus.arc1.dto.NextWorkoutInfoDTO;
import com.arcus.arc1.dto.WorkoutResponseDTO;
import com.arcus.arc1.levelDeterminer.WeightAssignmentService;
import com.arcus.arc1.levelDeterminer.WeightAssignmentService.WorkoutAdjustment;
import com.arcus.arc1.SetLog.SetLogRepo;
import com.arcus.arc1.SetLog.SetLogEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
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
    private final ExerciseLibraryRepo exerciseLibraryRepo;
    private final UserProfileRepo userProfileRepo;
    private final WorkoutExerciseTemplateRepository workoutExerciseTemplateRepository;
    private final SetLogRepo setLogRepo;

    @Autowired
    public WorkoutGenerationService(
            WorkoutTemplateRepo templateRepo,
            TemplateExerciseRepo exerciseRepo,
            WorkoutSessionRepo workoutSessionRepo,
            ExerciseSessionRepo exerciseSessionRepo,
            WeightAssignmentService weightAssignmentService,
            ExerciseLibraryRepo exerciseLibraryRepo,
            UserProfileRepo userProfileRepo,
            WorkoutExerciseTemplateRepository workoutExerciseTemplateRepository,
            SetLogRepo setLogRepo
    ) {
        this.templateRepo = templateRepo;
        this.exerciseRepo = exerciseRepo;
        this.workoutSessionRepo = workoutSessionRepo;
        this.exerciseSessionRepo = exerciseSessionRepo;
        this.weightAssignmentService = weightAssignmentService;
        this.exerciseLibraryRepo = exerciseLibraryRepo;
        this.userProfileRepo = userProfileRepo;
        this.workoutExerciseTemplateRepository = workoutExerciseTemplateRepository;
        this.setLogRepo = setLogRepo;
    }

    /**
     * Generates a new workout for a user with intelligently assigned weights.
     * Also updates the user's profile with workout statistics.
     *
     * @param userId User ID
     * @param level User's fitness level
     * @return WorkoutResponseDTO with exercises and assigned weights
     */
    public WorkoutResponseDTO generateWorkout(Long userId, String level, String goal) {

        List<ExerciseDTO> exerciseDTOList = new ArrayList<>();


        // Determine which day to generate based on user's last completed workout day
        int nextDay = determineNextDay(userId, level, goal);

        // Fetch template for this level and day
        WorkoutTemplateEntity template =
                templateRepo.findByLevelAndGoalAndDayNumber(
                        level,
                        goal,
                        nextDay
                ).orElseThrow(() -> new RuntimeException(
                        "No template found for level=" + level + ", goal=" + goal + ", day=" + nextDay));

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
            es.setTempo(te.getTempo());
            // Link to the exercise library so substitution is possible
            if (te.getExerciseLibraryId() != null) {
                es.setExerciseLibraryId(te.getExerciseLibraryId());
            }

            // Intelligently assign weight, sets, and rep range
            WorkoutAdjustment adjustment = assignAdjustmentForExercise(userId, level, te, session.getId());
            es.setTargetWeight(adjustment.weight());
            es.setSets(adjustment.sets());
            es.setRepMin(adjustment.repMin());
            es.setRepMax(adjustment.repMax());

            es = exerciseSessionRepo.save(es);

            // Calculate total sets and weight for profile update
            totalSets += adjustment.sets();
            // Weight = weight × average reps (using rep max as estimate)
            totalWeightCalculated += adjustment.weight() * adjustment.repMax();

            // Build response DTO
            ExerciseDTO exerciseDTO = new ExerciseDTO();
            exerciseDTO.setExerciseName(es.getExerciseName());
            exerciseDTO.setTargetWeight(es.getTargetWeight());
            exerciseDTO.setRepMin(es.getRepMin());
            exerciseDTO.setRepMax(es.getRepMax());
            exerciseDTO.setSets(es.getSets());
            exerciseDTO.setTempo(es.getTempo());
            exerciseDTO.setTip(te.getTip());

            exerciseDTOList.add(exerciseDTO);
        }

        // Update user profile with workout statistics
        updateUserProfile(userId, exercises.size(), totalSets, totalWeightCalculated);

        return new WorkoutResponseDTO(session.getId(), level, nextDay, exerciseDTOList);
    }

    /**
     * Generates a custom workout based on explicit muscle requests.
     * Picks exercises by muscle area order and saves exercise sessions WITHOUT weights.
     * Weights will be assigned later when the user starts the workout or via existing dynamic logic.
     */
    public WorkoutResponseDTO generateCustomWorkout(com.arcus.arc1.dto.GenerateWorkoutRequest request) {
        Long userId = request.getUserId();
        String level = request.getLevel();
        String goal = request.getGoal();
        Integer dayNumber = request.getDayNumber() != null ? request.getDayNumber() : 0;
        List<ExerciseDTO> result = new ArrayList<>();
        List<WorkoutExerciseTemplateEntity> templateEntities = new ArrayList<>();
        List<ExerciseLibraryEntity> allExerciseEntities = new ArrayList<>();

        for(String muscleRequest : request.getRequestedMuscles()) {
            List<ExerciseLibraryEntity> exerciseLibraryEntities =
                    exerciseLibraryRepo.findByMuscleGroupIgnoreCaseAndLevelIgnoreCase(
                            muscleRequest, level);
            if("beginner".equalsIgnoreCase(level)){
                if(result.isEmpty()){
                    exerciseLibraryEntities = pickRandom(exerciseLibraryEntities, 3);
                }
                else{
                    exerciseLibraryEntities = pickRandom(exerciseLibraryEntities, 2);
                }
            }
            else{
                exerciseLibraryEntities = pickRandom(exerciseLibraryEntities, 3);
            }
            allExerciseEntities.addAll(exerciseLibraryEntities);
            exerciseLibraryEntities.forEach(exerciseLibraryEntity -> {
                ExerciseDTO edto = new ExerciseDTO();
                edto.setExerciseName(exerciseLibraryEntity.getName());
                edto.setRepMax(exerciseLibraryEntity.getRepMax());
                edto.setRepMin(exerciseLibraryEntity.getRepMin());
                edto.setSets(exerciseLibraryEntity.getSets());
                edto.setMuscleArea(exerciseLibraryEntity.getMuscleArea());
                edto.setSecondaryMuscleGroup(exerciseLibraryEntity.getSecondaryMuscles());
                result.add(edto);
                // Store in WorkoutExerciseTemplateEntity
                WorkoutExerciseTemplateEntity entity = new WorkoutExerciseTemplateEntity();
                entity.setUserId(userId != null ? userId.intValue() : null);
                entity.setDayNumber(dayNumber);
                entity.setExerciseLibraryId(exerciseLibraryEntity.getId() != null ? exerciseLibraryEntity.getId().intValue() : null);
                entity.setCreatedAt(ZonedDateTime.now());
                edto.setExerciseTemplateSessionID(exerciseLibraryEntity.getId());
                templateEntities.add(entity);
            });
        }
        // Save all generated template entities
        workoutExerciseTemplateRepository.saveAll(templateEntities);
        // Assign weights for first set of each exercise
        for (int i = 0; i < templateEntities.size(); i++) {
            WorkoutExerciseTemplateEntity template = templateEntities.get(i);
            ExerciseDTO edto = result.get(i);
            List<SetLogEntity> logs = setLogRepo.findByWorkoutExerciseTemplateIdOrderBySetNumberAsc(template.getId());
            double weight;
            if (!logs.isEmpty()) {
                weight = logs.stream().mapToDouble(SetLogEntity::getWeight).average().orElse(0.0);
            } else {
                // Find the matching ExerciseLibraryEntity for this template
                ExerciseLibraryEntity exLib = allExerciseEntities.get(i);
                boolean isCompound = isCompoundExercise(exLib.getName());
                weight = weightAssignmentService.assignBaseWeight(level, exLib.getName(), isCompound);
            }
            edto.setTargetWeight(weight);
        }
        WorkoutResponseDTO response = new WorkoutResponseDTO(123L, level, dayNumber, result);
        return response;
    }

    // small helper to pick up to 'n' random elements preserving randomness
    private <T> List<T> pickRandom(List<T> source, int n) {
        if (source == null || source.isEmpty() || n <= 0) return java.util.Collections.emptyList();
        // Stream.toList() (and some other factory methods) may return an immutable list.
        // Copy into a mutable list before shuffling to avoid UnsupportedOperationException.
        List<T> mutable = new ArrayList<>(source);
        java.util.Collections.shuffle(mutable);
        return mutable.subList(0, Math.min(n, mutable.size()));
    }


    /**
     * Determines the next workout day for the user based on their history.
     * Looks at the user's last completed workout day from their profile,
     * then returns the next day in the cycle. Wraps back to day 1 after
     * completing all days in the program.
     *
     * @param userId User ID
     * @param level  User's fitness level
     * @param goal   User's fitness goal
     * @return The next day number to generate
     */
    private int determineNextDay(Long userId, String level, String goal) {
        // Find max day number for this level and goal
        int maxDay = templateRepo.findMaxDayNumberByLevelAndGoal(level, goal).orElse(1);

        // Look up the user's last completed workout day from their profile
        UserProfileEntity profile = userProfileRepo.findByUserId(userId).orElse(null);

        if (profile == null || profile.getLastWorkoutDay() == null || profile.getLastWorkoutDay() == 0) {
            // New user or no history — start at day 1
            return 1;
        }

        int lastDay = profile.getLastWorkoutDay();

        // Next day, cycling back to 1 after completing all days
        return (lastDay >= maxDay) ? 1 : lastDay + 1;
    }

    /**
     * Updates user profile with workout statistics after generating a workout.
     *
     * @param userId User ID
     * @param totalExercises Number of exercises in the workout
     * @param totalSets Total number of sets in the workout
     * @param totalWeight Estimated total weight to be lifted
     */
    private void updateUserProfile(Long userId, int totalExercises, int totalSets,
                                   double totalWeight) {
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
     * Assigns weight, sets, and rep range for an exercise based on user history and level.
     * For new users, returns template defaults with base weight.
     * For returning users, dynamically adjusts all three based on performance.
     *
     * @param userId User ID
     * @param level User's level
     * @param templateExercise Template exercise with default rep ranges and sets
     * @return WorkoutAdjustment with weight, sets, repMin, repMax
     */
    private WorkoutAdjustment assignAdjustmentForExercise(Long userId, String level,
                                                           TemplateExerciseEntity templateExercise,
                                                           Long currentWorkoutSessionId) {
        String exerciseName = templateExercise.getExerciseName();

        List<ExerciseSessionEntity> previousSessions = exerciseSessionRepo
                .findRecentExercisesByUserAndName(userId, exerciseName, currentWorkoutSessionId)
                .stream()
                .limit(3)
                .toList();

        System.out.println("[WEIGHT-DEBUG] Exercise: '" + exerciseName +
                "' | userId: " + userId +
                "' | excludeSessionId: " + currentWorkoutSessionId +
                " | previousSessions found: " + previousSessions.size());

        if (previousSessions.isEmpty()) {
            // New user or first time doing this exercise — assign base weight, keep template sets/reps
            boolean isCompound = isCompoundExercise(exerciseName);
            Double baseWeight = weightAssignmentService.assignBaseWeight(level, exerciseName, isCompound);
            System.out.println("[WEIGHT-DEBUG] No history for '" + exerciseName + "' → base weight: " + baseWeight);
            return new WorkoutAdjustment(baseWeight, templateExercise.getSets(),
                    templateExercise.getRepMin(), templateExercise.getRepMax());
        }

        // Returning user — dynamically adjust weight, sets, and rep range based on performance
        Double currentWeight = previousSessions.getFirst().getTargetWeight();
        System.out.println("[WEIGHT-DEBUG] History found for '" + exerciseName +
                "' | currentWeight: " + currentWeight +
                " | sessionIds: " + previousSessions.stream()
                    .map(s -> String.valueOf(s.getId()))
                    .reduce((a, b) -> a + "," + b).orElse("none"));

        WorkoutAdjustment result = weightAssignmentService.assignDynamicAdjustment(
                previousSessions,
                currentWeight,
                templateExercise.getSets(),
                templateExercise.getRepMin(),
                templateExercise.getRepMax()
        );

        System.out.println("[WEIGHT-DEBUG] Result for '" + exerciseName +
                "' → weight: " + result.weight() +
                " | sets: " + result.sets() +
                " | repMin: " + result.repMin() +
                " | repMax: " + result.repMax());

        return result;
    }

    /**
     * Assign weights for all exercises in a workout session.
     * This uses the existing dynamic adjustment logic and persists targetWeight, sets and rep ranges.
     * Returns a list of ExerciseDTO for the updated exercises.
     */
    public List<com.arcus.arc1.dto.ExerciseDTO> assignWeightsForSession(Long workoutSessionId) {
        WorkoutSessionEntity session = workoutSessionRepo.findById(workoutSessionId)
                .orElseThrow(() -> new RuntimeException("Workout session not found: " + workoutSessionId));

        Long userId = session.getUserId();
        UserProfileEntity profile = userProfileRepo.findByUserId(userId).orElse(null);
        String level = (profile != null && profile.getCurrentLevel() != null) ? profile.getCurrentLevel() : "beginner";

        List<ExerciseSessionEntity> exercises = exerciseSessionRepo.findByWorkoutSessionId(workoutSessionId);
        List<com.arcus.arc1.dto.ExerciseDTO> result = new ArrayList<>();

        for (ExerciseSessionEntity es : exercises) {
            // Build a pseudo-template to reuse adjustment logic
            TemplateExerciseEntity pseudo = new TemplateExerciseEntity();
            pseudo.setExerciseName(es.getExerciseName());
            pseudo.setSets(es.getSets() != null ? es.getSets() : 3);
            pseudo.setRepMin(es.getRepMin() != null ? es.getRepMin() : 8);
            pseudo.setRepMax(es.getRepMax() != null ? es.getRepMax() : 12);
            pseudo.setTempo(es.getTempo());

            WorkoutAdjustment adj = assignAdjustmentForExercise(userId, level, pseudo, workoutSessionId);

            es.setTargetWeight(adj.weight());
            es.setSets(adj.sets());
            es.setRepMin(adj.repMin());
            es.setRepMax(adj.repMax());
            exerciseSessionRepo.save(es);

            com.arcus.arc1.dto.ExerciseDTO dto = new com.arcus.arc1.dto.ExerciseDTO();
            dto.setExerciseName(es.getExerciseName());
            dto.setTargetWeight(es.getTargetWeight());
            dto.setRepMin(es.getRepMin());
            dto.setRepMax(es.getRepMax());
            dto.setSets(es.getSets());
            dto.setTempo(es.getTempo());
            result.add(dto);
        }

        return result;
    }

    /**
     * Returns info about the user's next workout and their last workout details.
     * Looks up the last workout session from the workout_session table,
     * resolves the template name from the template_id, and also computes the next workout.
     *
     * @param userId User ID
     * @param level  User's fitness level
     * @return NextWorkoutInfoDTO with next and last workout details
     */
    public NextWorkoutInfoDTO getNextWorkoutInfo(Long userId, String level) {
        String goal = "muscle_gain";
        int nextDay = determineNextDay(userId, level, goal);

        WorkoutTemplateEntity nextTemplate = templateRepo.findByLevelAndGoalAndDayNumber(level, goal, nextDay)
                .orElseThrow(() -> new RuntimeException(
                        "No template found for level=" + level + ", goal=" + goal + ", day=" + nextDay));

        // Look up the user's most recent COMPLETED workout session for last workout details
        String lastWorkoutName = null;
        Integer lastDayNumber = null;
        LocalDateTime lastWorkoutDate = null;
        boolean lastWorkoutCompleted = false;
        Double lastWorkoutTotalWeight = null;

        WorkoutSessionEntity lastSession = workoutSessionRepo
                .findTopByUserIdAndCompletedTrueOrderByCreatedAtDesc(userId)
                .orElse(null);

        if (lastSession != null) {
            lastWorkoutDate = lastSession.getCreatedAt();
            lastWorkoutCompleted = lastSession.isCompleted();
            lastWorkoutTotalWeight = lastSession.getTotalWeight();

            // Resolve the template name from the last session's templateId
            WorkoutTemplateEntity lastTemplate = templateRepo.findById(lastSession.getTemplateId())
                    .orElse(null);
            if (lastTemplate != null) {
                lastWorkoutName = lastTemplate.getName();
                lastDayNumber = lastTemplate.getDayNumber();
            }
        }

        return new NextWorkoutInfoDTO(
                nextTemplate.getName(), nextDay,
                lastWorkoutName, lastDayNumber,
                lastWorkoutDate, lastWorkoutCompleted,
                lastWorkoutTotalWeight,
                List.of(nextTemplate.getMuscleGroups().split(","))
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

    void finishWorkout(Long userId, double totalWeight){
        // Update Workout session entity with status and total weight.
        WorkoutSessionEntity workoutSessionEntity =
                workoutSessionRepo.findTopByUserIdAndCompletedFalseOrderByCreatedAtDesc((long) userId)
                .orElseThrow(() -> new RuntimeException("No active workout session found for userId: " + userId));
        workoutSessionEntity.setCompleted(true);
        workoutSessionEntity.setTotalWeight(totalWeight);
        workoutSessionRepo.save(workoutSessionEntity);
    }
}
