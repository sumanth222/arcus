package com.arcus.arc1.dto;

/**
 * Response DTO for GET /nutrition/target
 */
public class NutritionTargetDTO {

    private Long userId;

    /** Recommended daily protein intake (grams) */
    private Double proteinTargetGrams;

    /** Lower calorie bound (kcal/day) */
    private Integer calorieTargetMin;

    /** Upper calorie bound (kcal/day) */
    private Integer calorieTargetMax;

    /** Body weight used for calculation (kg) */
    private Double weightKg;

    /** Fitness goal: muscle_gain | weight_loss | maintain */
    private String fitnessGoal;

    /** Brief human-readable summary */
    private String summary;

    public NutritionTargetDTO() {}

    public NutritionTargetDTO(Long userId, Double proteinTargetGrams, Integer calorieTargetMin,
                               Integer calorieTargetMax, Double weightKg, String fitnessGoal, String summary) {
        this.userId = userId;
        this.proteinTargetGrams = proteinTargetGrams;
        this.calorieTargetMin = calorieTargetMin;
        this.calorieTargetMax = calorieTargetMax;
        this.weightKg = weightKg;
        this.fitnessGoal = fitnessGoal;
        this.summary = summary;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Double getProteinTargetGrams() { return proteinTargetGrams; }
    public void setProteinTargetGrams(Double proteinTargetGrams) { this.proteinTargetGrams = proteinTargetGrams; }

    public Integer getCalorieTargetMin() { return calorieTargetMin; }
    public void setCalorieTargetMin(Integer calorieTargetMin) { this.calorieTargetMin = calorieTargetMin; }

    public Integer getCalorieTargetMax() { return calorieTargetMax; }
    public void setCalorieTargetMax(Integer calorieTargetMax) { this.calorieTargetMax = calorieTargetMax; }

    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }

    public String getFitnessGoal() { return fitnessGoal; }
    public void setFitnessGoal(String fitnessGoal) { this.fitnessGoal = fitnessGoal; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}

