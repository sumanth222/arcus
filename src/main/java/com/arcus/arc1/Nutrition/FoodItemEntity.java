package com.arcus.arc1.Nutrition;

import jakarta.persistence.*;

/**
 * Static catalog of simple food items with protein and calorie estimates.
 * Seeded once at startup; supports quick recommendations without user logging.
 *
 * category values: veg | non_veg | shake
 */
@Entity
@Table(name = "food_item")
public class FoodItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    /** Approximate protein per typical serving (grams) */
    @Column(nullable = false)
    private Double proteinGrams;

    /** Approximate calories per typical serving (kcal) */
    @Column(nullable = false)
    private Integer caloriesKcal;

    /** veg | non_veg | shake */
    @Column(nullable = false)
    private String category;

    /** Human-readable serving size, e.g. "2 eggs", "100g chicken breast" */
    @Column(nullable = false)
    private String servingSize;

    public FoodItemEntity() {}

    public FoodItemEntity(String name, Double proteinGrams, Integer caloriesKcal,
                          String category, String servingSize) {
        this.name = name;
        this.proteinGrams = proteinGrams;
        this.caloriesKcal = caloriesKcal;
        this.category = category;
        this.servingSize = servingSize;
    }

    // Getters & Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getProteinGrams() { return proteinGrams; }
    public void setProteinGrams(Double proteinGrams) { this.proteinGrams = proteinGrams; }

    public Integer getCaloriesKcal() { return caloriesKcal; }
    public void setCaloriesKcal(Integer caloriesKcal) { this.caloriesKcal = caloriesKcal; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getServingSize() { return servingSize; }
    public void setServingSize(String servingSize) { this.servingSize = servingSize; }
}

