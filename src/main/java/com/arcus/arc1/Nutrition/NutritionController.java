package com.arcus.arc1.Nutrition;

import com.arcus.arc1.dto.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the Nutrition Lite feature.
 *
 * All endpoints are stateless from the caller's perspective —
 * just pass userId (and optionally workoutSessionId).
 *
 * GET /nutrition/target          → daily protein + calorie targets
 * GET /nutrition/post-workout    → post-workout protein/carb guidance
 * GET /nutrition/insights        → list of heuristic nutrition insights
 * GET /nutrition/quick-options   → ready-to-eat food combos
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/nutrition")
public class NutritionController {

    private final NutritionService nutritionService;

    public NutritionController(NutritionService nutritionService) {
        this.nutritionService = nutritionService;
    }

    /**
     * Returns the user's personalised daily nutrition targets.
     *
     * Example:
     *   GET /nutrition/target?userId=5
     */
    @GetMapping("/target")
    public NutritionTargetDTO getNutritionTarget(@RequestParam Long userId) {
        return nutritionService.getNutritionTarget(userId);
    }

    /**
     * Returns post-workout nutrition guidance based on session intensity.
     *
     * Example:
     *   GET /nutrition/post-workout?userId=5&workoutSessionId=42
     */
    @GetMapping("/post-workout")
    public PostWorkoutNutritionDTO getPostWorkoutNutrition(
            @RequestParam Long userId,
            @RequestParam Long workoutSessionId) {
        return nutritionService.getPostWorkoutNutrition(userId, workoutSessionId);
    }

    /**
     * Returns up to 10 recent heuristic nutrition insights for the user.
     *
     * Example:
     *   GET /nutrition/insights?userId=5
     */
    @GetMapping("/insights")
    public List<NutritionInsightDTO> getInsights(@RequestParam Long userId) {
        return nutritionService.getInsights(userId);
    }

    /**
     * Returns curated ready-to-eat food combinations (~25–40 g protein each).
     *
     * Example:
     *   GET /nutrition/quick-options?userId=5
     */
    @GetMapping("/quick-options")
    public List<QuickNutritionOptionsDTO> getQuickOptions(@RequestParam Long userId) {
        return nutritionService.getQuickOptions(userId);
    }
}

