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
import java.util.Optional;

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
        UserProfileEntity profileForSplit = userProfileRepo.findByUserId(userId).orElse(null);
        String split = (profileForSplit != null && profileForSplit.getWorkoutSplit() != null)
                ? profileForSplit.getWorkoutSplit() : null;

        int nextDay = determineNextDay(userId, level, goal, split);

        // Fetch template for this level, goal, split and day
        WorkoutTemplateEntity template = resolveTemplate(level, goal, split, nextDay);

        // Reuse existing incomplete session for this template if one exists
        WorkoutSessionEntity session = workoutSessionRepo
                .findTop1ByUserIdAndTemplateIdAndCompletedFalseOrderByCreatedAtDesc(userId, template.getId())
                .orElse(null);

        boolean isExistingSession = (session != null);

        if (!isExistingSession) {
            session = new WorkoutSessionEntity();
            session.setUserId(userId);
            session.setTemplateId(template.getId());
            session.setCreatedAt(LocalDateTime.now());
            session.setCompleted(false);
            session = workoutSessionRepo.save(session);
        } else {
            // Reuse the session entity but wipe its old exercise sessions so exercises
            // are always freshly generated (e.g. split or muscle group may have changed)
            List<ExerciseSessionEntity> stale = exerciseSessionRepo.findByWorkoutSessionId(session.getId());
            if (!stale.isEmpty()) {
                exerciseSessionRepo.deleteAll(stale);
            }
        }

        // Fetch exercises from template (needed for tips and defaults)
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
     * If an incomplete session with the same template already exists, it is reused.
     */
    public WorkoutResponseDTO generateCustomWorkout(com.arcus.arc1.dto.GenerateWorkoutRequest request) {
        Long userId = request.getUserId();
        String level = request.getLevel();
        String goal = request.getGoal();
        String split = request.getSplit();
        Integer dayNumber = request.getLastWorkoutDay() != null ? request.getLastWorkoutDay() : 0;

        // ── Resolve template first so we can check for an existing session ────
        WorkoutTemplateEntity workoutTemplate = resolveTemplate(level, goal, split, dayNumber);

        // ── Reuse existing incomplete session if one exists ────────────────────
        WorkoutSessionEntity session = workoutSessionRepo
                .findTop1ByUserIdAndTemplateIdAndCompletedFalseOrderByCreatedAtDesc(userId, workoutTemplate.getId())
                .orElse(null);

        Long existingSessionId = session != null ? session.getId() : null;

        if (existingSessionId != null) {
            // Wipe stale template entities for this user/day so exercises are always
            // freshly picked (split or muscle-group may have changed since last generation)
            List<WorkoutExerciseTemplateEntity> stale =
                    workoutExerciseTemplateRepository.findByUserIdAndDayNumberOrderByCreatedAtDesc(
                            userId != null ? userId.intValue() : null, dayNumber);
            if (!stale.isEmpty()) {
                workoutExerciseTemplateRepository.deleteAll(stale);
            }
        }

        // ── Always generate fresh exercises ───────────────────────────────────
        List<ExerciseDTO> result = new ArrayList<>();
        List<WorkoutExerciseTemplateEntity> templateEntities = new ArrayList<>();
        List<ExerciseLibraryEntity> allExerciseEntities = new ArrayList<>();

        List<String> muscles = request.getRequestedMuscles();

        // ── Total exercise count by level ──────────────────────────────────────
        boolean isAdvancedOrIntermediate = "intermediate".equalsIgnoreCase(level)
                || "advanced".equalsIgnoreCase(level);
        int totalExercises = isAdvancedOrIntermediate ? 6 : 5;

        // ── Allocate exercises per muscle group ────────────────────────────────
        int[] allocation = distributeExercises(totalExercises, muscles.size());

        for (int m = 0; m < muscles.size(); m++) {
            String muscleRequest = muscles.get(m);
            int countForMuscle = allocation[m];
            if (countForMuscle <= 0) continue;

            List<ExerciseLibraryEntity> exerciseLibraryEntities =
                    exerciseLibraryRepo.findByMuscleGroupAndLevelContains(muscleRequest, level);

            exerciseLibraryEntities = pickRandom(exerciseLibraryEntities, countForMuscle);

            allExerciseEntities.addAll(exerciseLibraryEntities);
            exerciseLibraryEntities.forEach(exerciseLibraryEntity -> {
                ExerciseDTO edto = new ExerciseDTO();
                edto.setExerciseName(exerciseLibraryEntity.getName());
                edto.setRepMax(exerciseLibraryEntity.getRepMax());
                edto.setRepMin(exerciseLibraryEntity.getRepMin());
                edto.setSets(exerciseLibraryEntity.getSets());
                edto.setMuscleArea(exerciseLibraryEntity.getMuscleArea());
                edto.setSecondaryMuscleGroup(exerciseLibraryEntity.getSecondaryMuscles());
                edto.setTip(exerciseLibraryEntity.getTip());
                edto.setVideoUrl(exerciseLibraryEntity.getVideoUrl());
                result.add(edto);

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

        // Assign weights for each exercise
        for (int i = 0; i < templateEntities.size(); i++) {
            WorkoutExerciseTemplateEntity template = templateEntities.get(i);
            ExerciseDTO edto = result.get(i);
            List<SetLogEntity> logs = setLogRepo.findByWorkoutExerciseTemplateIdOrderBySetNumberAsc(template.getId());
            double weight;
            if (!logs.isEmpty()) {
                weight = logs.stream().mapToDouble(SetLogEntity::getWeight).average().orElse(0.0);
            } else {
                ExerciseLibraryEntity exLib = allExerciseEntities.get(i);
                weight = resolveBaseWeight(exLib, level);
            }
            edto.setTargetWeight(weight);
        }

        // Reuse existing session or create a new one
        WorkoutSessionEntity finalSession;
        if (existingSessionId != null) {
            finalSession = workoutSessionRepo.findById(existingSessionId)
                    .orElseGet(() -> {
                        WorkoutSessionEntity s = new WorkoutSessionEntity();
                        s.setUserId(userId);
                        s.setTemplateId(workoutTemplate.getId());
                        s.setCreatedAt(LocalDateTime.now());
                        s.setCompleted(false);
                        return workoutSessionRepo.save(s);
                    });
        } else {
            finalSession = new WorkoutSessionEntity();
            finalSession.setUserId(userId);
            finalSession.setTemplateId(workoutTemplate.getId());
            finalSession.setCreatedAt(LocalDateTime.now());
            finalSession.setCompleted(false);
            finalSession = workoutSessionRepo.save(finalSession);
        }

        return new WorkoutResponseDTO(finalSession.getId(), level, dayNumber, result);
    }

    /**
     * Distributes {@code total} exercises across {@code numGroups} muscle groups,
     * giving priority to the first (major) group.
     *
     * Rules:
     *  - Major group gets ⌈total × 0.6⌉ exercises (at least 2 if possible).
     *  - Every minor group gets at least 1 exercise.
     *  - If total is too small to give major its share AND give each minor at least 1,
     *    major is reduced so every minor still gets 1.
     *
     * Examples:
     *  total=5, groups=1  → [5]
     *  total=5, groups=2  → [3, 2]      (chest + triceps, beginner)
     *  total=6, groups=2  → [4, 2]      (chest + triceps, intermediate)
     *  total=5, groups=3  → [3, 1, 1]
     *  total=6, groups=3  → [4, 1, 1]
     */
    /**
     * Resolves a WorkoutTemplateEntity using goal + day, optionally scoped to a split.
     * Level is intentionally excluded — for a given split and day the template is the
     * same regardless of whether the user is a beginner or advanced.
     */
    private WorkoutTemplateEntity resolveTemplate(String level, String goal, String split, int dayNumber) {
        if (split != null && !split.isBlank()) {
            Optional<WorkoutTemplateEntity> bySplitAndDay =
                    templateRepo.findByGoalAndSplitAndDayNumber(goal, split, dayNumber);
            if (bySplitAndDay.isPresent()) return bySplitAndDay.get();
        }
        return templateRepo.findByGoalAndDayNumber(goal, dayNumber)
                .orElseThrow(() -> new RuntimeException(
                        "No template found for goal=" + goal
                                + (split != null && !split.isBlank() ? ", split=" + split : "")
                                + ", day=" + dayNumber));
    }

    private int[] distributeExercises(int total, int numGroups) {
        if (numGroups <= 0) return new int[0];
        int[] alloc = new int[numGroups];
        if (numGroups == 1) {
            alloc[0] = total;
            return alloc;
        }
        int minorGroups = numGroups - 1;
        // Major gets ~60%, but ensure each minor muscle gets at least 1
        int major = (int) Math.ceil(total * 0.6);
        if (major + minorGroups > total) {
            major = total - minorGroups; // shrink major so every minor gets ≥1
        }
        alloc[0] = Math.max(1, major);
        int remaining = total - alloc[0];
        int basePerMinor = remaining / minorGroups;
        int extras       = remaining % minorGroups;
        for (int i = 1; i < numGroups; i++) {
            alloc[i] = basePerMinor + (i <= extras ? 1 : 0);
        }
        return alloc;
    }

    /**
     * Returns the pre-configured base weight for an exercise based on the user's level.
     * Uses the beginner_weight / intermediate_weight / advanced_weight columns from
     * exercise_library.  Falls back to 10 kg if the column is null.
     */
    private double resolveBaseWeight(ExerciseLibraryEntity exLib, String level) {
        if (exLib == null) return 10.0;
        String lvl = level != null ? level.toLowerCase() : "beginner";
        Double weight;
        if (lvl.contains("advanced")) {
            weight = exLib.getAdvancedWeight();
        } else if (lvl.contains("intermediate")) {
            weight = exLib.getIntermediateWeight();
        } else {
            weight = exLib.getBeginnerWeight();
        }
        return (weight != null && weight > 0) ? weight : 10.0;
    }

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
    private int determineNextDay(Long userId, String level, String goal, String split) {
        // Max day is determined by goal + split only — level doesn't change the cycle length
        int maxDay;
        if (split != null && !split.isBlank()) {
            maxDay = templateRepo.findMaxDayNumberByGoalAndSplit(goal, split)
                    .orElse(templateRepo.findMaxDayNumberByGoal(goal).orElse(1));
        } else {
            maxDay = templateRepo.findMaxDayNumberByGoal(goal).orElse(1);
        }

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
        // Resolve goal and split from the user's profile instead of hardcoding
        UserProfileEntity userProfile = userProfileRepo.findByUserId(userId).orElse(null);
        String goal  = (userProfile != null && userProfile.getFitnessGoal()  != null)
                ? userProfile.getFitnessGoal()  : "muscle_gain";
        String split = (userProfile != null && userProfile.getWorkoutSplit() != null)
                ? userProfile.getWorkoutSplit() : null;

        int nextDay = determineNextDay(userId, level, goal, split);

        WorkoutTemplateEntity nextTemplate = resolveTemplate(level, goal, split, nextDay);

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

        // Also fetch the previous occurrence of the same template (if any) to compute weight change
        Double previousWorkoutTotalWeight = null;
        Double lastWorkoutWeightChange = null;
        if (lastSession != null && lastSession.getTemplateId() != null) {
            // Fetch at most two entries (most recent first) to minimize DB work
            List<WorkoutSessionEntity> sameTemplateSessions =
                    workoutSessionRepo.findTop2ByUserIdAndTemplateIdAndCompletedTrueOrderByCreatedAtDesc(
                            userId, lastSession.getTemplateId());

            if (sameTemplateSessions != null && sameTemplateSessions.size() >= 2) {
                // index 0 is the most recent (lastSession), index 1 is the previous occurrence
                WorkoutSessionEntity previous = sameTemplateSessions.get(1);
                previousWorkoutTotalWeight = previous.getTotalWeight();
                if (lastWorkoutTotalWeight != null && previousWorkoutTotalWeight != null) {
                    lastWorkoutWeightChange = lastWorkoutTotalWeight - previousWorkoutTotalWeight;
                }
            }
        }

        // Compute percent change safely: (last - prev) / prev * 100
        Double lastWorkoutWeightChangePercent = null;
        if (lastWorkoutWeightChange != null && previousWorkoutTotalWeight != null && previousWorkoutTotalWeight != 0.0) {
            double raw = (lastWorkoutWeightChange / previousWorkoutTotalWeight) * 100.0;
            // round to 1 decimal place for UI friendliness
            lastWorkoutWeightChangePercent = Math.round(raw * 10.0) / 10.0;
        }

        return new NextWorkoutInfoDTO(
                nextTemplate.getName(), nextDay,
                lastWorkoutName, lastDayNumber,
                lastWorkoutDate, lastWorkoutCompleted,
                lastWorkoutTotalWeight,
                previousWorkoutTotalWeight,
                lastWorkoutWeightChange,
                lastWorkoutWeightChangePercent,
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

    public void finishWorkout(Long userId, double totalWeight){
        // Update Workout session entity with status and total weight.
        // If no active session exists (e.g. rest day), silently skip.
        WorkoutSessionEntity workoutSessionEntity =
                workoutSessionRepo.findTopByUserIdAndCompletedFalseOrderByCreatedAtDesc((long) userId)
                .orElse(null);
        if (workoutSessionEntity == null) {
            // No active workout session — expected on a rest day, nothing to finish.
            return;
        }
        workoutSessionEntity.setCompleted(true);
        workoutSessionEntity.setTotalWeight(totalWeight);
        workoutSessionRepo.save(workoutSessionEntity);
    }
}
