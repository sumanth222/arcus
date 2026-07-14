package com.arcus.arc1.AiWorkoutPlan;

import com.arcus.arc1.ExerciseLibrary.ExerciseLibraryEntity;
import com.arcus.arc1.ExerciseLibrary.ExerciseLibraryRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Endpoints for managing the AI-generated workout plan.
 *
 *  POST /ai-plan/generate?userId={id}   → trigger (or re-trigger) plan generation
 *  GET  /ai-plan/{userId}               → view the stored plan (for debugging / UI preview)
 */
@CrossOrigin
@RestController
@RequestMapping("/ai-plan")
public class AiWorkoutPlanController {

    private final AiWorkoutPlanService aiWorkoutPlanService;
    private final AiWorkoutPlanRepository aiWorkoutPlanRepository;
    private final ExerciseLibraryRepo exerciseLibraryRepo;

    public AiWorkoutPlanController(AiWorkoutPlanService aiWorkoutPlanService,
                                   AiWorkoutPlanRepository aiWorkoutPlanRepository,
                                   ExerciseLibraryRepo exerciseLibraryRepo) {
        this.aiWorkoutPlanService = aiWorkoutPlanService;
        this.aiWorkoutPlanRepository = aiWorkoutPlanRepository;
        this.exerciseLibraryRepo = exerciseLibraryRepo;
    }

    /**
     * Triggers synchronous plan generation for the given user.
     * Call this after profile creation (or after a significant profile update).
     * The response confirms how many exercises were stored.
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generatePlan(@RequestParam Long userId) {
        String result = aiWorkoutPlanService.generateAndStorePlan(userId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", userId);
        body.put("result", result);
        boolean success = result != null && result.startsWith("Plan generated");
        body.put("success", success);
        return success
                ? ResponseEntity.ok(body)
                : ResponseEntity.status(500).body(body);
    }

    /**
     * Returns the stored AI plan for a user, grouped by day.
     * Each entry includes the exercise name, muscle group, sets, and rep range.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getPlan(@PathVariable Long userId) {
        List<AiWorkoutPlanEntity> entries =
                aiWorkoutPlanRepository.findByUserIdOrderByDayNumberAscIdAsc(userId);

        if (entries.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "planExists", false,
                    "message", "No AI plan found. Call POST /ai-plan/generate?userId=" + userId + " to create one."
            ));
        }

        // Group by day
        Map<Integer, List<Map<String, Object>>> byDay = new LinkedHashMap<>();
        for (AiWorkoutPlanEntity entry : entries) {
            byDay.computeIfAbsent(entry.getDayNumber(), k -> new ArrayList<>());

            Map<String, Object> exerciseInfo = new LinkedHashMap<>();
            exerciseInfo.put("exerciseLibraryId", entry.getExerciseLibraryId());
            exerciseInfo.put("muscleGroup", entry.getMuscleGroup());
            exerciseInfo.put("sets", entry.getSets());
            exerciseInfo.put("repMin", entry.getRepMin());
            exerciseInfo.put("repMax", entry.getRepMax());

            // Enrich with exercise name from library
            if (entry.getExerciseLibraryId() != null) {
                exerciseLibraryRepo.findById(entry.getExerciseLibraryId()).ifPresent(lib -> {
                    exerciseInfo.put("exerciseName", lib.getName());
                    exerciseInfo.put("muscleArea", lib.getMuscleArea());
                    exerciseInfo.put("equipment", lib.getEquipment());
                    exerciseInfo.put("tip", lib.getTip());
                    exerciseInfo.put("videoUrl", lib.getVideoUrl());
                });
            }

            byDay.get(entry.getDayNumber()).add(exerciseInfo);
        }

        // Build day-level response
        List<Map<String, Object>> days = new ArrayList<>();
        byDay.forEach((dayNumber, exercises) -> {
            String dayName = entries.stream()
                    .filter(e -> e.getDayNumber().equals(dayNumber))
                    .findFirst()
                    .map(AiWorkoutPlanEntity::getDayName)
                    .orElse("Day " + dayNumber);

            Map<String, Object> dayMap = new LinkedHashMap<>();
            dayMap.put("dayNumber", dayNumber);
            dayMap.put("dayName", dayName);
            dayMap.put("exercises", exercises);
            days.add(dayMap);
        });

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("userId", userId);
        response.put("planExists", true);
        response.put("totalDays", days.size());
        response.put("totalExercises", entries.size());
        response.put("generatedAt", entries.get(0).getCreatedAt());
        response.put("days", days);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes the stored plan so the next call to /ai-plan/generate starts fresh.
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> deletePlan(@PathVariable Long userId) {
        aiWorkoutPlanRepository.deleteByUserId(userId);
        return ResponseEntity.ok(Map.of("userId", userId, "deleted", true));
    }
}

