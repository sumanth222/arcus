package com.arcus.arc1.dto;

public class FoodSuggestionDTO {

    private String name;
    private Double proteinGrams;
    private Integer caloriesKcal;
    private String category;
    private String servingSize;

    public FoodSuggestionDTO() {}

    public FoodSuggestionDTO(String name, Double proteinGrams, Integer caloriesKcal,
                              String category, String servingSize) {
        this.name = name;
        this.proteinGrams = proteinGrams;
        this.caloriesKcal = caloriesKcal;
        this.category = category;
        this.servingSize = servingSize;
    }

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

