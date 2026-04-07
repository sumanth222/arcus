package com.arcus.arc1.Nutrition;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Stores the computed daily nutrition targets for a user.
 * Re-calculated whenever the user's body weight or goal changes.
 */
@Entity
@Table(name = "nutrition_profile")
public class NutritionProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    /** Recommended daily protein intake in grams (1.6–2.2 g/kg × bodyweight) */
    @Column(nullable = false)
    private Double proteinTargetGrams;

    /** Lower bound of daily calorie range */
    @Column(nullable = false)
    private Integer calorieTargetMin;

    /** Upper bound of daily calorie range */
    @Column(nullable = false)
    private Integer calorieTargetMax;

    /** Body weight at the time this profile was last calculated */
    @Column(nullable = false)
    private Double weightKgSnapshot;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    public NutritionProfileEntity() {}

    // Getters & Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Double getProteinTargetGrams() { return proteinTargetGrams; }
    public void setProteinTargetGrams(Double proteinTargetGrams) { this.proteinTargetGrams = proteinTargetGrams; }

    public Integer getCalorieTargetMin() { return calorieTargetMin; }
    public void setCalorieTargetMin(Integer calorieTargetMin) { this.calorieTargetMin = calorieTargetMin; }

    public Integer getCalorieTargetMax() { return calorieTargetMax; }
    public void setCalorieTargetMax(Integer calorieTargetMax) { this.calorieTargetMax = calorieTargetMax; }

    public Double getWeightKgSnapshot() { return weightKgSnapshot; }
    public void setWeightKgSnapshot(Double weightKgSnapshot) { this.weightKgSnapshot = weightKgSnapshot; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}

