package com.arcus.arc1.Nutrition;

import com.arcus.arc1.ExerciseSession.ExerciseSessionEntity;
import com.arcus.arc1.ExerciseSession.ExerciseSessionRepo;
import com.arcus.arc1.SetLog.SetLogEntity;
import com.arcus.arc1.SetLog.SetLogRepo;
import com.arcus.arc1.UserProfile.UserProfileEntity;
import com.arcus.arc1.UserProfile.UserProfileRepo;
import com.arcus.arc1.WorkoutSession.WorkoutSessionEntity;
import com.arcus.arc1.WorkoutSession.WorkoutSessionRepo;
import com.arcus.arc1.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core service for Nutrition Lite.
 *
 * All calculations are deterministic and stateless — no ML, no manual food logging.
 *
 * Protein target formula  : weight × multiplier (by level)
 *   beginner     → 1.6 g/kg
 *   intermediate → 1.8 g/kg
 *   advanced     → 2.0 g/kg
 *   muscle_gain  → bonus 0.2 g/kg on top of level multiplier (capped at 2.2)
 *
 * Calorie formula         : weight × 31 kcal (midpoint maintenance)
 *   muscle_gain  → +300 kcal
 *   weight_loss  → -300 kcal
 *   maintain     → no change
 *
 * Post-workout intensity (by volume = Σ weight×reps across all sets in session):
 *   LOW    < 2 500 kg
 *   MEDIUM 2 500 – 6 000 kg
 *   HIGH   > 6 000 kg
 */
@Service
public class NutritionService {

    private static final Logger log = LoggerFactory.getLogger(NutritionService.class);

    // ── intensity thresholds (kg·reps) ──────────────────────────────────────
    private static final double VOLUME_LOW_THRESHOLD    = 2_500.0;
    private static final double VOLUME_MEDIUM_THRESHOLD = 6_000.0;

    // ── protein factors (g / kg bodyweight) ─────────────────────────────────
    private static final Map<String, Double> LEVEL_PROTEIN_FACTOR = Map.of(
            "beginner",     1.6,
            "intermediate", 1.8,
            "advanced",     2.0
    );

    private final UserProfileRepo          userProfileRepo;
    private final WorkoutSessionRepo       workoutSessionRepo;
    private final ExerciseSessionRepo      exerciseSessionRepo;
    private final SetLogRepo               setLogRepo;
    private final NutritionProfileRepo     nutritionProfileRepo;
    private final FoodItemRepo             foodItemRepo;
    private final NutritionInsightRepo     insightRepo;

    public NutritionService(UserProfileRepo userProfileRepo,
                            WorkoutSessionRepo workoutSessionRepo,
                            ExerciseSessionRepo exerciseSessionRepo,
                            SetLogRepo setLogRepo,
                            NutritionProfileRepo nutritionProfileRepo,
                            FoodItemRepo foodItemRepo,
                            NutritionInsightRepo insightRepo) {
        this.userProfileRepo       = userProfileRepo;
        this.workoutSessionRepo    = workoutSessionRepo;
        this.exerciseSessionRepo   = exerciseSessionRepo;
        this.setLogRepo            = setLogRepo;
        this.nutritionProfileRepo  = nutritionProfileRepo;
        this.foodItemRepo          = foodItemRepo;
        this.insightRepo           = insightRepo;
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /nutrition/target
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Returns (and persists) the user's daily nutrition targets.
     * Re-calculates whenever called so the values always reflect the latest
     * body-weight and fitness goal stored in the profile.
     */
    public NutritionTargetDTO getNutritionTarget(Long userId) {
        UserProfileEntity profile = getProfile(userId);

        double weight = resolveWeight(profile);
        String goal   = profile.getFitnessGoal() != null
                        ? profile.getFitnessGoal().toLowerCase() : "maintain";
        String level  = profile.getCurrentLevel() != null
                        ? profile.getCurrentLevel().toLowerCase() : "beginner";

        double proteinFactor = LEVEL_PROTEIN_FACTOR.getOrDefault(level, 1.6);
        // muscle_gain users benefit from slightly higher protein
        if (goal.contains("muscle") || goal.contains("gain") || goal.contains("bulk")) {
            proteinFactor = Math.min(proteinFactor + 0.2, 2.2);
        }

        double proteinTarget = Math.round(weight * proteinFactor * 10.0) / 10.0;

        // Maintenance calories = weight × 31 (reasonable midpoint)
        int maintenance = (int) Math.round(weight * 31);
        int calorieMin, calorieMax;
        if (goal.contains("muscle") || goal.contains("gain") || goal.contains("bulk")) {
            calorieMin = maintenance + 250;
            calorieMax = maintenance + 350;
        } else if (goal.contains("loss") || goal.contains("cut") || goal.contains("weight_loss")) {
            calorieMin = maintenance - 350;
            calorieMax = maintenance - 250;
        } else {
            // maintain
            calorieMin = maintenance - 100;
            calorieMax = maintenance + 100;
        }

        // Persist so the UI can cache it
        NutritionProfileEntity np = nutritionProfileRepo.findByUserId(userId).orElse(new NutritionProfileEntity());
        np.setUserId(userId);
        np.setProteinTargetGrams(proteinTarget);
        np.setCalorieTargetMin(calorieMin);
        np.setCalorieTargetMax(calorieMax);
        np.setWeightKgSnapshot(weight);
        np.setLastUpdated(LocalDateTime.now());
        nutritionProfileRepo.save(np);

        String summary = buildTargetSummary(weight, proteinTarget, calorieMin, calorieMax, goal);

        return new NutritionTargetDTO(userId, proteinTarget, calorieMin, calorieMax, weight,
                                      profile.getFitnessGoal(), summary);
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /nutrition/post-workout?workoutSessionId=
    // ════════════════════════════════════════════════════════════════════════

    public PostWorkoutNutritionDTO getPostWorkoutNutrition(Long userId, Long workoutSessionId) {
        // Validate user exists
        getProfile(userId);

        workoutSessionRepo.findById(workoutSessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Workout session not found: " + workoutSessionId));

        // -- Compute total training volume ----------------------------------------
        double totalVolume = computeSessionVolume(workoutSessionId);

        // -- Intensity bucket -------------------------------------------------------
        String intensity;
        if (totalVolume < VOLUME_LOW_THRESHOLD) {
            intensity = "LOW";
        } else if (totalVolume < VOLUME_MEDIUM_THRESHOLD) {
            intensity = "MEDIUM";
        } else {
            intensity = "HIGH";
        }

        // -- Protein recommendation ------------------------------------------------
        //   LOW    → 25 g  (light session, muscles still need basic repair)
        //   MEDIUM → 32 g  (solid session)
        //   HIGH   → 40 g  (heavy compound session — maximum acute MPS window)
        int proteinRec;
        String carbLevel;
        String rationale;
        switch (intensity) {
            case "HIGH" -> {
                proteinRec = 40;
                carbLevel  = "HIGH";
                rationale  = "Heavy session detected. Prioritise 40 g protein + high-GI carbs within 30 min to replenish glycogen and maximise muscle protein synthesis.";
            }
            case "MEDIUM" -> {
                proteinRec = 32;
                carbLevel  = "MEDIUM";
                rationale  = "Solid workout. Aim for 32 g protein with moderate carbs to support recovery and refuel muscle glycogen.";
            }
            default -> {
                proteinRec = 25;
                carbLevel  = "LOW";
                rationale  = "Light session. 25 g protein is sufficient. Keep carbs moderate and stay hydrated.";
            }
        }

        // -- Food suggestions -------------------------------------------------------
        List<FoodSuggestionDTO> suggestions = pickFoodSuggestions(proteinRec);

        // -- Auto-generate a post-workout insight -----------------------------------
        generatePostWorkoutInsight(userId, intensity);

        PostWorkoutNutritionDTO dto = new PostWorkoutNutritionDTO();
        dto.setWorkoutSessionId(workoutSessionId);
        dto.setIntensity(intensity);
        dto.setTotalVolume(Math.round(totalVolume * 100.0) / 100.0);
        dto.setProteinRecommendationGrams(proteinRec);
        dto.setCarbSuggestionLevel(carbLevel);
        dto.setRationale(rationale);
        dto.setFoodSuggestions(suggestions);
        return dto;
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /nutrition/insights?userId=
    // ════════════════════════════════════════════════════════════════════════

    public List<NutritionInsightDTO> getInsights(Long userId) {
        // Ensure the user exists
        getProfile(userId);

        // Evaluate heuristic insights before returning
        evaluateWorkoutConsistencyInsights(userId);

        return insightRepo.findTop10ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(i -> new NutritionInsightDTO(i.getId(), i.getMessage(), i.getType(), i.getCreatedAt()))
                .collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /nutrition/quick-options?userId=
    // ════════════════════════════════════════════════════════════════════════

    public List<QuickNutritionOptionsDTO> getQuickOptions(Long userId) {
        UserProfileEntity profile = getProfile(userId);
        String goal  = profile.getFitnessGoal() != null ? profile.getFitnessGoal().toLowerCase() : "maintain";

        // Shuffle so every call returns a fresh, varied selection
        List<FoodItemEntity> all = new ArrayList<>(foodItemRepo.findAll());
        Collections.shuffle(all);

        List<FoodItemEntity> vegItems    = all.stream().filter(f -> "veg".equalsIgnoreCase(f.getCategory())).collect(Collectors.toList());
        List<FoodItemEntity> nonVegItems = all.stream().filter(f -> "non_veg".equalsIgnoreCase(f.getCategory())).collect(Collectors.toList());
        List<FoodItemEntity> shakeItems  = all.stream().filter(f -> "shake".equalsIgnoreCase(f.getCategory())).collect(Collectors.toList());

        List<QuickNutritionOptionsDTO> combos = new ArrayList<>();

        // ── Combo 1: Goal-aligned power combo ───────────────────────────────
        // weight_loss → best protein-per-calorie; muscle_gain/strength → highest protein
        List<FoodItemEntity> goalSorted = new ArrayList<>(all);
        if ("weight_loss".equals(goal)) {
            goalSorted.sort(Comparator.comparingDouble(f -> -(f.getProteinGrams() / Math.max(f.getCaloriesKcal(), 1))));
        } else {
            goalSorted.sort(Comparator.comparingDouble(f -> -f.getProteinGrams()));
        }
        List<FoodSuggestionDTO> powerItems = goalSorted.stream()
                .limit(3)
                .map(this::toSuggestionDTO)
                .collect(Collectors.toList());
        String powerName = switch (goal) {
            case "weight_loss" -> "Lean Protein Combo";
            case "muscle_gain" -> "Muscle Builder Combo";
            case "strength"    -> "Strength Fuel Combo";
            default            -> "High Protein Combo";
        };
        if (!powerItems.isEmpty()) combos.add(buildCombo(powerName, powerItems));

        // ── Combo 2: Mixed plate (veg + non_veg together) ───────────────────
        List<FoodSuggestionDTO> mixedItems = new ArrayList<>();
        if (!nonVegItems.isEmpty()) mixedItems.add(toSuggestionDTO(nonVegItems.get(0)));
        if (!vegItems.isEmpty())    mixedItems.add(toSuggestionDTO(vegItems.get(0)));
        if (vegItems.size() > 1)    mixedItems.add(toSuggestionDTO(vegItems.get(1)));
        if (!mixedItems.isEmpty()) combos.add(buildCombo("Mixed Protein Plate", mixedItems));

        // ── Combo 3: Quick shake + whole food ───────────────────────────────
        List<FoodSuggestionDTO> shakeCombo = new ArrayList<>();
        if (!shakeItems.isEmpty()) shakeCombo.add(toSuggestionDTO(shakeItems.get(0)));
        // complement the shake with a medium-protein solid food
        all.stream()
                .filter(f -> !"shake".equalsIgnoreCase(f.getCategory()) && f.getProteinGrams() >= 10.0)
                .findFirst()
                .ifPresent(f -> shakeCombo.add(toSuggestionDTO(f)));
        if (!shakeCombo.isEmpty()) combos.add(buildCombo("Quick Shake + Food Combo", shakeCombo));

        // ── Combo 4: Indian kitchen combo (randomly varied each call) ───────
        // Pick 3 random veg staples — shuffle already applied so always different
        List<FoodSuggestionDTO> indianItems = vegItems.stream()
                .limit(3)
                .map(this::toSuggestionDTO)
                .collect(Collectors.toList());
        if (!indianItems.isEmpty()) combos.add(buildCombo("Indian Kitchen Combo", indianItems));

        // ── Combo 5: Budget protein (high protein, ≤ 230 kcal per serving) ──
        List<FoodSuggestionDTO> budgetItems = all.stream()
                .filter(f -> f.getProteinGrams() >= 12.0 && f.getCaloriesKcal() <= 230)
                .limit(3)
                .map(this::toSuggestionDTO)
                .collect(Collectors.toList());
        if (!budgetItems.isEmpty()) combos.add(buildCombo("Budget Protein Combo", budgetItems));

        return combos;
    }

    // ════════════════════════════════════════════════════════════════════════
    // Private helpers
    // ════════════════════════════════════════════════════════════════════════

    private UserProfileEntity getProfile(Long userId) {
        return userProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User profile not found for userId: " + userId));
    }

    /**
     * Resolves a usable weight value. Falls back to a conservative 70 kg if
     * the profile has no weight stored yet.
     */
    private double resolveWeight(UserProfileEntity profile) {
        if (profile.getWeightKg() != null && profile.getWeightKg() > 0) {
            return profile.getWeightKg();
        }
        return 70.0; // sensible default
    }

    /**
     * Computes the total training volume for a session.
     * Uses set logs linked via workout_exercise_template.
     * Falls back to estimating from exercise sessions if no set logs exist.
     */
    private double computeSessionVolume(Long workoutSessionId) {
        List<ExerciseSessionEntity> exercises = exerciseSessionRepo.findByWorkoutSessionId(workoutSessionId);

        double totalVolume = 0.0;
        for (ExerciseSessionEntity ex : exercises) {
            boolean countedFromLogs = false;
            if (ex.getWorkoutExerciseTemplateId() != null) {
                List<SetLogEntity> logs = setLogRepo
                        .findByWorkoutExerciseTemplateIdOrderBySetNumberAsc(ex.getWorkoutExerciseTemplateId());
                for (SetLogEntity log : logs) {
                    if (log.getWeight() != null && log.getReps() != null) {
                        totalVolume += log.getWeight() * log.getReps();
                        countedFromLogs = true;
                    }
                }
            }
            // Fallback: use target weight × rep midpoint × completed sets ONLY when no actual set logs exist
            if (!countedFromLogs && ex.getTargetWeight() != null && ex.getCompletedSets() != null
                    && ex.getRepMin() != null && ex.getRepMax() != null) {
                int midReps = (ex.getRepMin() + ex.getRepMax()) / 2;
                totalVolume += ex.getTargetWeight() * midReps * ex.getCompletedSets();
            }
        }
        return totalVolume;
    }

    /**
     * Picks 3–5 food items whose combined protein roughly matches the target.
     * Prioritises non-veg then veg then shakes to give variety.
     */
    private List<FoodSuggestionDTO> pickFoodSuggestions(int targetProtein) {
        List<FoodItemEntity> all = foodItemRepo.findAll();
        if (all.isEmpty()) return Collections.emptyList();

        // Sort by protein descending so we pick the most protein-dense items first
        all.sort((a, b) -> Double.compare(b.getProteinGrams(), a.getProteinGrams()));

        List<FoodSuggestionDTO> result = new ArrayList<>();
        double accumulated = 0.0;
        for (FoodItemEntity item : all) {
            if (result.size() >= 5) break;
            result.add(toSuggestionDTO(item));
            accumulated += item.getProteinGrams();
            if (accumulated >= targetProtein) break;
        }
        return result;
    }

    private FoodSuggestionDTO toSuggestionDTO(FoodItemEntity e) {
        return new FoodSuggestionDTO(e.getName(), e.getProteinGrams(),
                e.getCaloriesKcal(), e.getCategory(), e.getServingSize());
    }

    private QuickNutritionOptionsDTO buildCombo(String name, List<FoodSuggestionDTO> items) {
        double protein  = items.stream().mapToDouble(FoodSuggestionDTO::getProteinGrams).sum();
        int    calories = items.stream().mapToInt(FoodSuggestionDTO::getCaloriesKcal).sum();
        return new QuickNutritionOptionsDTO(name, Math.round(protein * 10.0) / 10.0, calories, items);
    }

    private String buildTargetSummary(double weight, double protein, int calMin, int calMax, String goal) {
        return String.format(
                "For your %.0f kg body weight and '%s' goal: aim for %.0f g protein/day " +
                "and %d–%d kcal/day.",
                weight, goal, protein, calMin, calMax);
    }

    // ── Insight generators ──────────────────────────────────────────────────

    private void generatePostWorkoutInsight(Long userId, String intensity) {
        String message;
        String type;
        if ("HIGH".equals(intensity)) {
            message = "Great workout! Make sure to consume 35–40 g of protein within 30–45 minutes to maximise muscle protein synthesis.";
            type = "POST_WORKOUT_SUCCESS";
        } else if ("MEDIUM".equals(intensity)) {
            message = "Solid session! Aim for 30 g protein post-workout along with some carbs to refuel.";
            type = "POST_WORKOUT_INFO";
        } else {
            message = "Light session completed. 25 g protein is sufficient. Stay hydrated and get enough sleep for recovery.";
            type = "POST_WORKOUT_INFO";
        }

        // Upsert: one POST_WORKOUT_ row per user — update in place, don't keep creating new rows
        NutritionInsightEntity insight = insightRepo
                .findTopByUserIdAndTypeStartingWithOrderByCreatedAtDesc(userId, "POST_WORKOUT_")
                .orElse(new NutritionInsightEntity());
        insight.setUserId(userId);
        insight.setMessage(message);
        insight.setType(type);
        insight.setCreatedAt(LocalDateTime.now());
        insightRepo.save(insight);
    }

    /**
     * Computes the user's workout frequency for the past 7 days and upserts a single
     * CONSISTENCY_ insight row. Upsert means we UPDATE the existing row rather than
     * inserting a new one — this keeps the table clean (one row per user).
     */
    private void evaluateWorkoutConsistencyInsights(Long userId) {
        // Query only completed sessions directly from DB
        List<WorkoutSessionEntity> completedSessions = workoutSessionRepo
                .findByUserIdAndCompletedTrueOrderByCreatedAtDesc(userId);

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long completedLast7Days = completedSessions.stream()
                .filter(s -> s.getCreatedAt() != null && s.getCreatedAt().isAfter(sevenDaysAgo))
                .count();

        log.info("[NutritionInsight] userId={} | totalCompleted={} | completedLast7Days={}",
                userId, completedSessions.size(), completedLast7Days);

        String message;
        String type;
        if (completedLast7Days >= 4) {
            message = "🔥 You've completed " + completedLast7Days + " workouts this week — great consistency! Make sure your protein intake is on point to support your progress.";
            type = "CONSISTENCY_SUCCESS";
        } else if (completedLast7Days >= 2) {
            message = "You've worked out " + completedLast7Days + " times this week. Try to squeeze in one more session and pair it with adequate protein for better results.";
            type = "CONSISTENCY_INFO";
        } else if (completedLast7Days == 1) {
            message = "Only 1 workout this week so far. Even on rest days, try to hit your daily protein target to preserve muscle.";
            type = "CONSISTENCY_WARNING";
        } else {
            message = "No workouts logged this week yet. Fuel up and get started — consistent effort is the key to progress!";
            type = "CONSISTENCY_WARNING";
        }

        // Upsert: one CONSISTENCY_ row per user — update in place, don't keep creating new rows
        NutritionInsightEntity insight = insightRepo
                .findTopByUserIdAndTypeStartingWithOrderByCreatedAtDesc(userId, "CONSISTENCY_")
                .orElse(new NutritionInsightEntity());
        insight.setUserId(userId);
        insight.setMessage(message);
        insight.setType(type);
        insight.setCreatedAt(LocalDateTime.now());
        insightRepo.save(insight);
    }
}









