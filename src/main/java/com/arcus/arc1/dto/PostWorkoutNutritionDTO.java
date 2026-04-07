package com.arcus.arc1.dto;

import java.util.List;

/**
 * Response DTO for GET /nutrition/post-workout
 */
public class PostWorkoutNutritionDTO {

    private Long workoutSessionId;

    /** Workout intensity: LOW | MEDIUM | HIGH */
    private String intensity;

    /** Total training volume (kg × reps) used to determine intensity */
    private Double totalVolume;

    /** Recommended post-workout protein intake (grams) */
    private Integer proteinRecommendationGrams;

    /** Suggested carbohydrate intake level: LOW | MEDIUM | HIGH */
    private String carbSuggestionLevel;

    /** Short explanation for the recommendation */
    private String rationale;

    /** 3–5 concrete food items to hit the protein target */
    private List<FoodSuggestionDTO> foodSuggestions;

    public PostWorkoutNutritionDTO() {}

    public Long getWorkoutSessionId() { return workoutSessionId; }
    public void setWorkoutSessionId(Long workoutSessionId) { this.workoutSessionId = workoutSessionId; }

    public String getIntensity() { return intensity; }
    public void setIntensity(String intensity) { this.intensity = intensity; }

    public Double getTotalVolume() { return totalVolume; }
    public void setTotalVolume(Double totalVolume) { this.totalVolume = totalVolume; }

    public Integer getProteinRecommendationGrams() { return proteinRecommendationGrams; }
    public void setProteinRecommendationGrams(Integer proteinRecommendationGrams) {
        this.proteinRecommendationGrams = proteinRecommendationGrams;
    }

    public String getCarbSuggestionLevel() { return carbSuggestionLevel; }
    public void setCarbSuggestionLevel(String carbSuggestionLevel) { this.carbSuggestionLevel = carbSuggestionLevel; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }

    public List<FoodSuggestionDTO> getFoodSuggestions() { return foodSuggestions; }
    public void setFoodSuggestions(List<FoodSuggestionDTO> foodSuggestions) { this.foodSuggestions = foodSuggestions; }
}

