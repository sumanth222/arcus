package com.arcus.arc1.dto;

/**
 * Request body for PUT /user/profile/{userId}.
 * All fields are optional — only non-null values will be applied.
 */
public class UpdateProfileRequest {

    private String name;
    private String email;
    private String bio;
    private String fitnessGoal;
    private String currentLevel;
    private String workoutSplit;
    private Double heightCm;
    private Double weightKg;
    private Integer lastWorkoutDay;

    public UpdateProfileRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getFitnessGoal() { return fitnessGoal; }
    public void setFitnessGoal(String fitnessGoal) { this.fitnessGoal = fitnessGoal; }

    public String getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(String currentLevel) { this.currentLevel = currentLevel; }

    public String getWorkoutSplit() { return workoutSplit; }
    public void setWorkoutSplit(String workoutSplit) { this.workoutSplit = workoutSplit; }

    public Double getHeightCm() { return heightCm; }
    public void setHeightCm(Double heightCm) { this.heightCm = heightCm; }

    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }

    public Integer getLastWorkoutDay() { return lastWorkoutDay; }
    public void setLastWorkoutDay(Integer lastWorkoutDay) { this.lastWorkoutDay = lastWorkoutDay; }
}

