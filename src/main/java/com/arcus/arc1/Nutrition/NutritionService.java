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
        UserProfileEntity profile = getProfile(userId);

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
        // Ensure the user exists
        getProfile(userId);

        List<FoodItemEntity> allFoods = foodItemRepo.findAll();

        List<QuickNutritionOptionsDTO> combos = new ArrayList<>();

        // Combo 1 – Vegetarian power combo
        List<FoodSuggestionDTO> vegCombo = allFoods.stream()
                .filter(f -> "veg".equalsIgnoreCase(f.getCategory()))
                .limit(3)
                .map(this::toSuggestionDTO)
                .collect(Collectors.toList());
        if (!vegCombo.isEmpty()) {
            combos.add(buildCombo("Vegetarian protein combo", vegCombo));
        }

        // Combo 2 – Non-veg classic
        List<FoodSuggestionDTO> nonVegCombo = allFoods.stream()
                .filter(f -> "non_veg".equalsIgnoreCase(f.getCategory()))
                .limit(3)
                .map(this::toSuggestionDTO)
                .collect(Collectors.toList());
        if (!nonVegCombo.isEmpty()) {
            combos.add(buildCombo("Non-veg classic combo", nonVegCombo));
        }

        // Combo 3 – Shake + whole food
        List<FoodSuggestionDTO> shakeCombo = new ArrayList<>();
        allFoods.stream().filter(f -> "shake".equalsIgnoreCase(f.getCategory())).findFirst()
                .ifPresent(s -> shakeCombo.add(toSuggestionDTO(s)));
        allFoods.stream().filter(f -> "veg".equalsIgnoreCase(f.getCategory())).findFirst()
                .ifPresent(s -> shakeCombo.add(toSuggestionDTO(s)));
        if (!shakeCombo.isEmpty()) {
            combos.add(buildCombo("Quick shake combo", shakeCombo));
        }

        // Combo 4 – Mixed best protein-per-calorie
        List<FoodSuggestionDTO> mixedCombo = allFoods.stream()
                .sorted(Comparator.comparingDouble(f ->
                        -(f.getProteinGrams() / Math.max(f.getCaloriesKcal(), 1))))
                .limit(4)
                .map(this::toSuggestionDTO)
                .collect(Collectors.toList());
        combos.add(buildCombo("Best protein-per-calorie combo", mixedCombo));

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
            if (ex.getWorkoutExerciseTemplateId() != null) {
                List<SetLogEntity> logs = setLogRepo
                        .findByWorkoutExerciseTemplateIdOrderBySetNumberAsc(ex.getWorkoutExerciseTemplateId());
                for (SetLogEntity log : logs) {
                    if (log.getWeight() != null && log.getReps() != null) {
                        totalVolume += log.getWeight() * log.getReps();
                    }
                }
            }
            // Fallback: use target weight × rep midpoint × completed sets
            if (ex.getTargetWeight() != null && ex.getCompletedSets() != null
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
            type = "SUCCESS";
        } else if ("MEDIUM".equals(intensity)) {
            message = "Solid session! Aim for 30 g protein post-workout along with some carbs to refuel.";
            type = "INFO";
        } else {
            message = "Light session completed. 25 g protein is sufficient. Stay hydrated and get enough sleep for recovery.";
            type = "INFO";
        }
        insightRepo.save(new NutritionInsightEntity(userId, message, type));
    }

    /**
     * Generates heuristic insights based on recent workout frequency.
     * Only creates a new insight if a similar one hasn't been created in the past 7 days
     * (prevents spam — we check by looking at the most recent insight).
     */
    private void evaluateWorkoutConsistencyInsights(Long userId) {
        List<WorkoutSessionEntity> recentSessions = workoutSessionRepo
                .findByUserIdOrderByCreatedAtDesc(userId);

        long completedLast7Days = recentSessions.stream()
                .filter(WorkoutSessionEntity::isCompleted)
                .filter(s -> s.getCreatedAt() != null &&
                             s.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7)))
                .count();

        List<NutritionInsightEntity> existing = insightRepo.findTop10ByUserIdOrderByCreatedAtDesc(userId);
        boolean recentInsightExists = existing.stream()
                .anyMatch(i -> i.getCreatedAt().isAfter(LocalDateTime.now().minusDays(3)));

        if (recentInsightExists) return; // avoid insight spam

        String message;
        String type;
        if (completedLast7Days >= 4) {
            message = "🔥 You've completed " + completedLast7Days + " workouts this week — great consistency! Make sure your protein intake is on point to support your progress.";
            type = "SUCCESS";
        } else if (completedLast7Days >= 2) {
            message = "You've worked out " + completedLast7Days + " times this week. Try to squeeze in one more session and pair it with adequate protein for better results.";
            type = "INFO";
        } else if (completedLast7Days == 1) {
            message = "Only 1 workout this week so far. Even on rest days, try to hit your daily protein target to preserve muscle.";
            type = "WARNING";
        } else {
            message = "No workouts logged this week yet. Fuel up and get started — consistent effort is the key to progress!";
            type = "WARNING";
        }
        insightRepo.save(new NutritionInsightEntity(userId, message, type));
    }
}









