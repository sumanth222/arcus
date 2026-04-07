package com.arcus.arc1.dto;

import java.util.List;

/**
 * Response DTO for GET /nutrition/quick-options
 * Returns ready-to-eat combinations that together hit ~25–40 g protein.
 */
public class QuickNutritionOptionsDTO {

    /** Suggested combo label, e.g. "High-protein vegetarian combo" */
    private String comboName;

    /** Total estimated protein for this combo */
    private Double totalProteinGrams;

    /** Total estimated calories for this combo */
    private Integer totalCaloriesKcal;

    /** Individual food items in this combo */
    private List<FoodSuggestionDTO> items;

    public QuickNutritionOptionsDTO() {}

    public QuickNutritionOptionsDTO(String comboName, Double totalProteinGrams,
                                     Integer totalCaloriesKcal, List<FoodSuggestionDTO> items) {
        this.comboName = comboName;
        this.totalProteinGrams = totalProteinGrams;
        this.totalCaloriesKcal = totalCaloriesKcal;
        this.items = items;
    }

    public String getComboName() { return comboName; }
    public void setComboName(String comboName) { this.comboName = comboName; }

    public Double getTotalProteinGrams() { return totalProteinGrams; }
    public void setTotalProteinGrams(Double totalProteinGrams) { this.totalProteinGrams = totalProteinGrams; }

    public Integer getTotalCaloriesKcal() { return totalCaloriesKcal; }
    public void setTotalCaloriesKcal(Integer totalCaloriesKcal) { this.totalCaloriesKcal = totalCaloriesKcal; }

    public List<FoodSuggestionDTO> getItems() { return items; }
    public void setItems(List<FoodSuggestionDTO> items) { this.items = items; }
}

