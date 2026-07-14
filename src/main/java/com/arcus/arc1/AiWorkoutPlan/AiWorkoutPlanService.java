package com.arcus.arc1.AiWorkoutPlan;

import com.arcus.arc1.ExerciseLibrary.ExerciseLibraryEntity;
import com.arcus.arc1.ExerciseLibrary.ExerciseLibraryRepo;
import com.arcus.arc1.UserProfile.UserProfileEntity;
import com.arcus.arc1.UserProfile.UserProfileRepo;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Generates and stores a personalised AI workout plan for a user.
 *
 * NEW APPROACH:
 *  1. For each day in the user's split, determine target muscle groups.
 *  2. Fetch ALL matching exercises from exercise_library (filtered by level + equipment).
 *  3. Send that real exercise list to ChatGPT and ask it to pick the best 5-6.
 *  4. Store the chosen exercise_library IDs in ai_workout_plan.
 *
 * This means ChatGPT is doing intelligent selection from real data,
 * not guessing which muscles/equipment to target.
 */
@Service
public class AiWorkoutPlanService {

    private final AiWorkoutPlanRepository aiPlanRepository;
    private final ExerciseLibraryRepo exerciseLibraryRepo;
    private final UserProfileRepo userProfileRepo;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.model:gpt-4o-mini}")
    private String model;

    // ── Split → Day → Muscle Groups mapping ───────────────────────────────────
    private static final Map<String, Map<Integer, List<String>>> SPLIT_DAY_MUSCLES;

    static {
        SPLIT_DAY_MUSCLES = new LinkedHashMap<>();

        // Push-Pull-Legs
        Map<Integer, List<String>> ppl = new LinkedHashMap<>();
        ppl.put(1, List.of("chest", "shoulders", "triceps"));
        ppl.put(2, List.of("back", "biceps"));
        ppl.put(3, List.of("legs", "core"));
        SPLIT_DAY_MUSCLES.put("ppl", ppl);

        // Upper / Lower
        Map<Integer, List<String>> ul = new LinkedHashMap<>();
        ul.put(1, List.of("chest", "back", "shoulders", "biceps", "triceps"));
        ul.put(2, List.of("legs", "core"));
        SPLIT_DAY_MUSCLES.put("upper_lower", ul);

        // Full Body
        Map<Integer, List<String>> fb = new LinkedHashMap<>();
        fb.put(1, List.of("chest", "back", "legs", "shoulders"));
        fb.put(2, List.of("back", "legs", "chest", "core"));
        fb.put(3, List.of("shoulders", "legs", "biceps", "triceps"));
        SPLIT_DAY_MUSCLES.put("full_body", fb);

        // Bro Split
        Map<Integer, List<String>> bro = new LinkedHashMap<>();
        bro.put(1, List.of("chest"));
        bro.put(2, List.of("back"));
        bro.put(3, List.of("shoulders"));
        bro.put(4, List.of("biceps", "triceps"));
        bro.put(5, List.of("legs", "core"));
        SPLIT_DAY_MUSCLES.put("bro_split", bro);

        // Push / Pull
        Map<Integer, List<String>> pp = new LinkedHashMap<>();
        pp.put(1, List.of("chest", "shoulders", "triceps"));
        pp.put(2, List.of("back", "biceps"));
        SPLIT_DAY_MUSCLES.put("push_pull", pp);
    }

    public AiWorkoutPlanService(AiWorkoutPlanRepository aiPlanRepository,
                                ExerciseLibraryRepo exerciseLibraryRepo,
                                UserProfileRepo userProfileRepo) {
        this.aiPlanRepository = aiPlanRepository;
        this.exerciseLibraryRepo = exerciseLibraryRepo;
        this.userProfileRepo = userProfileRepo;
        this.objectMapper = new ObjectMapper();

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .responseTimeout(Duration.ofSeconds(90))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(90, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)));

        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Public API
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Called after profile creation. Runs on a background thread.
     */
    @Async
    public void generatePlanAsync(Long userId) {
        try {
            generateAndStorePlan(userId);
        } catch (Exception e) {
            System.err.println("[AI-PLAN] Background generation failed for userId=" + userId + ": " + e.getMessage());
        }
    }

    /**
     * Synchronous version — used by the manual-trigger controller endpoint.
     *
     * For each day in the user's split:
     *   1. Determine target muscle groups from the static split map.
     *   2. Fetch all matching exercises from exercise_library.
     *   3. Ask ChatGPT to choose the best 5-6 from that real list.
     *   4. Store chosen exercise IDs in ai_workout_plan.
     */
    @Transactional
    public String generateAndStorePlan(Long userId) {
        UserProfileEntity profile = userProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        aiPlanRepository.deleteByUserId(userId);

        String split          = profile.getWorkoutSplit()    != null ? profile.getWorkoutSplit().toLowerCase()    : "ppl";
        String level          = profile.getCurrentLevel()    != null ? profile.getCurrentLevel()                  : "beginner";
        String workoutLocation = profile.getWorkoutLocation() != null ? profile.getWorkoutLocation()              : "gym";

        int planDays = resolvePlanDays(split);
        Map<Integer, List<String>> dayMuscleMap = SPLIT_DAY_MUSCLES.getOrDefault(split, SPLIT_DAY_MUSCLES.get("ppl"));
        int exerciseCount = "beginner".equalsIgnoreCase(level) ? 5 : 6;

        List<AiWorkoutPlanEntity> allEntries = new ArrayList<>();

        for (int day = 1; day <= planDays; day++) {
            List<String> muscleGroups = dayMuscleMap.getOrDefault(day, List.of());
            if (muscleGroups.isEmpty()) continue;

            String dayName = buildDayName(split, day);

            // Step 1: Fetch real exercises from library
            List<ExerciseLibraryEntity> candidates = fetchCandidates(muscleGroups, level, workoutLocation);

            if (candidates.isEmpty()) {
                System.err.println("[AI-PLAN] No exercises found for day=" + day
                        + " muscles=" + muscleGroups + " level=" + level + " location=" + workoutLocation);
                continue;
            }

            System.out.println("[AI-PLAN] Day " + day + " (" + dayName + "): "
                    + candidates.size() + " candidates for muscles=" + muscleGroups);

            // Step 2: Ask ChatGPT to pick the best ones
            List<Long> chosenIds = askChatGptToChoose(candidates, exerciseCount, profile, dayName, muscleGroups, null);

            if (chosenIds.isEmpty()) {
                System.err.println("[AI-PLAN] ChatGPT returned no valid IDs for day=" + day + " — using random fallback.");
                chosenIds = randomFallback(candidates, exerciseCount);
            }

            // Step 3: Persist chosen exercises
            for (Long exId : chosenIds) {
                ExerciseLibraryEntity ex = exerciseLibraryRepo.findById(exId).orElse(null);
                if (ex == null) continue;

                AiWorkoutPlanEntity entry = new AiWorkoutPlanEntity();
                entry.setUserId(userId);
                entry.setDayNumber(day);
                entry.setDayName(dayName);
                entry.setExerciseLibraryId(ex.getId());
                entry.setMuscleGroup(ex.getMuscleGroup());
                entry.setSets(ex.getSets() > 0 ? ex.getSets() : 3);
                entry.setRepMin(ex.getRepMin() > 0 ? ex.getRepMin() : 8);
                entry.setRepMax(ex.getRepMax() > 0 ? ex.getRepMax() : 12);
                allEntries.add(entry);
            }
        }

        if (allEntries.isEmpty()) {
            return "No exercises could be selected — check exercise library data and level filters.";
        }

        aiPlanRepository.saveAll(allEntries);
        System.out.println("[AI-PLAN] Saved " + allEntries.size() + " exercises for userId=" + userId);
        return "Plan generated successfully — " + allEntries.size() + " exercises stored.";
    }

    /**
     * Called when a user completes a full cycle and returns to a day (new cycle refresh).
     * Fetches fresh exercises for that day's muscles, excludes previously done ones,
     * and asks ChatGPT to choose from the remaining candidates.
     */
    @Transactional
    public void refreshDayPlan(Long userId, Integer dayNumber, String dayName,
                                List<String> previousExerciseNames,
                                String level, String goal, String workoutLocation,
                               List<String> requestedMuscles) {
        System.out.println("[AI-PLAN] Refreshing day=" + dayNumber + " for userId=" + userId
                + " | previous=" + previousExerciseNames);

        UserProfileEntity profile = userProfileRepo.findByUserId(userId).orElse(null);
        if (profile == null) return;

        String split = profile.getWorkoutSplit() != null ? profile.getWorkoutSplit().toLowerCase() : "ppl";
        List<String> muscleGroups = requestedMuscles;

        // Fetch candidates and exclude previously done exercises
        List<ExerciseLibraryEntity> candidates = fetchCandidates(muscleGroups, level, workoutLocation);
        Set<String> prevNamesLower = (previousExerciseNames == null) ? Set.of()
                : previousExerciseNames.stream().map(String::toLowerCase).collect(Collectors.toSet());
        candidates = candidates.stream()
                .filter(e -> !prevNamesLower.contains(e.getName().toLowerCase()))
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            System.err.println("[AI-PLAN] Refresh: no new candidates for day=" + dayNumber + " — keeping old plan.");
            return;
        }

        int exerciseCount = "beginner".equalsIgnoreCase(level) ? 5 : 6;
        List<Long> chosenIds = askChatGptToChoose(candidates, exerciseCount, profile, dayName, muscleGroups, previousExerciseNames);

        if (chosenIds.isEmpty()) {
            chosenIds = randomFallback(candidates, exerciseCount);
        }

        List<AiWorkoutPlanEntity> newEntries = new ArrayList<>();
        for (Long exId : chosenIds) {
            ExerciseLibraryEntity ex = exerciseLibraryRepo.findById(exId).orElse(null);
            if (ex == null) continue;

            AiWorkoutPlanEntity entry = new AiWorkoutPlanEntity();
            entry.setUserId(userId);
            entry.setDayNumber(dayNumber);
            entry.setDayName(dayName);
            entry.setExerciseLibraryId(ex.getId());
            entry.setMuscleGroup(ex.getMuscleGroup());
            entry.setSets(ex.getSets() > 0 ? ex.getSets() : 3);
            entry.setRepMin(ex.getRepMin() > 0 ? ex.getRepMin() : 8);
            entry.setRepMax(ex.getRepMax() > 0 ? ex.getRepMax() : 12);
            newEntries.add(entry);
        }

        if (!newEntries.isEmpty()) {
            aiPlanRepository.deleteByUserIdAndDayNumber(userId, dayNumber);
            aiPlanRepository.saveAll(newEntries);
            System.out.println("[AI-PLAN] Day " + dayNumber + " refreshed with "
                    + newEntries.size() + " new exercises for userId=" + userId);
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Direct request-time selection (used by generateCustomWorkout)
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Selects exercises for the current workout request on-the-fly.
     * No stored plan is read or written — everything is derived from the incoming
     * request parameters (muscleGroups, level, workoutLocation, user profile).
     *
     * ChatGPT picks the best {@code count} exercises from our real library data.
     * Falls back to random selection if ChatGPT is unavailable.
     */
    public List<ExerciseLibraryEntity> selectExercisesForWorkout(
            List<String> muscleGroups, int count, UserProfileEntity profile, String workoutLocation) {

        if (muscleGroups == null || muscleGroups.isEmpty()) return List.of();

        String level = (profile != null && profile.getCurrentLevel() != null)
                ? profile.getCurrentLevel() : "beginner";
        if (workoutLocation == null) workoutLocation = "gym";

        List<ExerciseLibraryEntity> candidates = fetchCandidates(muscleGroups, level, workoutLocation);

        if (candidates.isEmpty()) {
            System.err.println("[AI-PLAN] No candidates found for muscles=" + muscleGroups
                    + " level=" + level + " location=" + workoutLocation);
            return List.of();
        }

        String dayLabel = muscleGroups.stream()
                .map(mg -> mg.substring(0, 1).toUpperCase() + mg.substring(1))
                .collect(Collectors.joining(", "));

        List<Long> chosenIds = askChatGptToChoose(candidates, count, profile, dayLabel, muscleGroups, null);

        if (chosenIds.isEmpty()) {
            System.err.println("[AI-PLAN] ChatGPT returned nothing for muscles=" + muscleGroups + " — random fallback.");
            List<ExerciseLibraryEntity> shuffled = new ArrayList<>(candidates);
            Collections.shuffle(shuffled);
            return shuffled.stream().limit(count).collect(Collectors.toList());
        }

        // Map chosen IDs back to entities in ChatGPT's preferred order
        Map<Long, ExerciseLibraryEntity> candidateMap = candidates.stream()
                .collect(Collectors.toMap(ExerciseLibraryEntity::getId, e -> e));
        return chosenIds.stream()
                .map(candidateMap::get)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Core Logic
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Fetches exercises from exercise_library for the given muscle groups,
     * filtered by user level and allowed equipment based on workout location.
     */
    private List<ExerciseLibraryEntity> fetchCandidates(List<String> muscleGroups,
                                                         String level,
                                                         String workoutLocation) {
        List<String> allowedEquipment = resolveAllowedEquipment(workoutLocation);
        List<ExerciseLibraryEntity> candidates = new ArrayList<>();

        for (String mg : muscleGroups) {
            List<ExerciseLibraryEntity> exercises = (allowedEquipment != null)
                    ? exerciseLibraryRepo.findByMuscleGroupAndLevelContainsAndEquipmentIn(mg, level, allowedEquipment)
                    : exerciseLibraryRepo.findByMuscleGroupAndLevelContains(mg, level);
            candidates.addAll(exercises);
        }

        return candidates;
    }

    /**
     * Sends the real exercise catalogue to ChatGPT and asks it to pick the best ones.
     * Returns exercise_library IDs chosen by ChatGPT, validated against the candidate set.
     */
    private List<Long> askChatGptToChoose(List<ExerciseLibraryEntity> candidates,
                                           int count,
                                           UserProfileEntity profile,
                                           String dayName,
                                           List<String> muscleGroups,
                                           List<String> excludeNames) {
        // Build compact catalogue JSON
        List<Map<String, Object>> catalog = candidates.stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.getId());
            m.put("name", e.getName());
            m.put("muscleGroup", e.getMuscleGroup());
            m.put("muscleArea", e.getMuscleArea() != null ? e.getMuscleArea() : "");
            m.put("equipment", e.getEquipment());
            m.put("category", e.getCategory());
            return m;
        }).toList();

        String catalogJson;
        try {
            catalogJson = objectMapper.writeValueAsString(catalog);
        } catch (Exception e) {
            System.err.println("[AI-PLAN] Failed to serialize exercise catalog: " + e.getMessage());
            return List.of();
        }

        String prompt = buildChoicePrompt(catalogJson, count, profile, dayName, muscleGroups, excludeNames);

        System.out.println("[AI-PLAN] Asking ChatGPT to choose " + count
                + " from " + candidates.size() + " exercises for: " + dayName);

        String rawJson = callChatGpt(prompt);
        if (rawJson == null) return List.of();

        rawJson = stripMarkdown(rawJson);

        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode chosenNode = root.path("chosen");
            if (!chosenNode.isArray() || chosenNode.isEmpty()) {
                System.err.println("[AI-PLAN] ChatGPT response missing 'chosen' array.");
                return List.of();
            }

            Set<Long> validIds = candidates.stream().map(ExerciseLibraryEntity::getId).collect(Collectors.toSet());
            List<Long> result = new ArrayList<>();

            for (JsonNode item : chosenNode) {
                long id = item.isObject() ? item.path("id").asLong(-1) : item.asLong(-1);
                if (id > 0 && validIds.contains(id)) {
                    result.add(id);
                } else {
                    System.err.println("[AI-PLAN] Ignoring unknown id=" + id + " from ChatGPT response.");
                }
            }

            System.out.println("[AI-PLAN] ChatGPT chose " + result.size()
                    + " valid exercises (out of " + chosenNode.size() + " returned).");
            return result.stream().limit(count).collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("[AI-PLAN] Parse error: " + e.getMessage()
                    + " | raw=" + rawJson.substring(0, Math.min(300, rawJson.length())));
            return List.of();
        }
    }

    /**
     * Builds the selection prompt — presents real exercises and asks ChatGPT to choose.
     */
    private String buildChoicePrompt(String catalogJson, int count, UserProfileEntity profile,
                                      String dayName, List<String> muscleGroups,
                                      List<String> excludeNames) {
        String level       = profile.getCurrentLevel()    != null ? profile.getCurrentLevel()    : "beginner";
        String goal        = profile.getFitnessGoal()     != null ? profile.getFitnessGoal()     : "muscle_gain";
        String injuries    = (profile.getInjuries()       != null && !profile.getInjuries().isBlank())       ? profile.getInjuries()       : "none";
        String weakMuscles = (profile.getWeakMuscleGroups() != null && !profile.getWeakMuscleGroups().isBlank()) ? profile.getWeakMuscleGroups() : "none";
        String avoidList   = (excludeNames != null && !excludeNames.isEmpty()) ? String.join(", ", excludeNames) : "none";
        String musclesStr  = String.join(", ", muscleGroups);

        return """
You are a professional strength and conditioning coach. \
Choose exactly %d exercises from the list below for today's session.

ATHLETE PROFILE:
- Level: %s | Goal: %s
- Today's workout: %s
- Target muscles: %s
- Weak/lagging muscles (give extra priority): %s
- Injuries (avoid stressing): %s
- Exercises done last cycle (do NOT repeat): %s

AVAILABLE EXERCISES (pick from these IDs only):
%s

RULES:
1. Choose the number of exercises carefully so as to be not too less for major muscles and not too many for minor muscles
also make sure to not have too many to tire the person. Keep the total under 7 or 8.
2. Cover ALL target muscle groups — don't neglect any.
3. Compound exercises before isolation.
4. Prioritise weak muscle groups with extra exercises.
5. Avoid exercises that stress injuries.
6. Do NOT include any exercise from the "done last cycle" list.
7. Vary muscleArea — avoid two exercises targeting the same sub-region.

Return ONLY this JSON (no markdown, no explanation):
{"chosen":[{"id":1,"name":"Exercise Name"}]}
""".formatted(count, level, goal, dayName, musclesStr, weakMuscles, injuries, avoidList, catalogJson, count);
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ────────────────────────────────────────────────────────────────────────────

    private List<Long> randomFallback(List<ExerciseLibraryEntity> candidates, int count) {
        List<ExerciseLibraryEntity> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled);
        return shuffled.stream().limit(count).map(ExerciseLibraryEntity::getId).collect(Collectors.toList());
    }

    private String buildDayName(String split, int day) {
        return switch (split.toLowerCase()) {
            case "ppl" -> switch (day) {
                case 1 -> "Push – Chest, Shoulders & Triceps";
                case 2 -> "Pull – Back & Biceps";
                case 3 -> "Legs & Core";
                default -> "Day " + day;
            };
            case "upper_lower" -> day == 1 ? "Upper Body" : "Lower Body & Core";
            case "full_body"   -> "Full Body – Session " + day;
            case "bro_split"   -> switch (day) {
                case 1 -> "Chest Day";
                case 2 -> "Back Day";
                case 3 -> "Shoulder Day";
                case 4 -> "Arms Day";
                case 5 -> "Legs & Core";
                default -> "Day " + day;
            };
            case "push_pull"   -> day == 1 ? "Push Day" : "Pull Day";
            default -> "Day " + day;
        };
    }

    private List<String> resolveAllowedEquipment(String location) {
        if (location == null) return null;
        return switch (location.toLowerCase().trim()) {
            case "home" -> List.of("dumbbell", "bodyweight", "resistance_band");
            case "gym"  -> List.of("barbell", "dumbbell", "cable", "machine", "smith_machine", "bodyweight", "ez_bar");
            default     -> null; // "both" — no filter
        };
    }

    private int resolvePlanDays(String split) {
        if (split == null) return 3;
        return switch (split.toLowerCase()) {
            case "ppl"                        -> 3;
            case "upper_lower", "upper/lower" -> 2;
            case "full_body"                  -> 3;
            case "bro_split", "bro"           -> 5;
            case "push_pull"                  -> 2;
            default                           -> 3;
        };
    }

    private String callChatGpt(String prompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content",
                                    "You are a professional fitness coach. Return ONLY valid JSON."),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.6,
                    "max_tokens", 1000
            );
            Map<?, ?> response = webClient.post().uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(body).retrieve().bodyToMono(Map.class).block();
            if (response == null) return null;
            @SuppressWarnings("unchecked")
            List<Map<?, ?>> choices = (List<Map<?, ?>>) response.get("choices");
            if (choices == null || choices.isEmpty()) return null;
            Map<?, ?> message = (Map<?, ?>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            System.err.println("[AI-PLAN] ChatGPT call failed: " + e.getMessage());
            return null;
        }
    }

    private String stripMarkdown(String raw) {
        if (raw == null) return null;
        raw = raw.trim();
        if (raw.startsWith("```")) {
            raw = raw.replaceFirst("```[a-zA-Z]*\\n?", "");
            if (raw.endsWith("```")) raw = raw.substring(0, raw.lastIndexOf("```"));
        }
        return raw.trim();
    }
}
