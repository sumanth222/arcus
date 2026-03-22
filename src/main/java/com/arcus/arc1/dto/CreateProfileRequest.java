package com.arcus.arc1.dto;

public class CreateProfileRequest {

    private Long userId;
    private String username;  // used to link UserCredentials if credentialsId not provided
    private String name;
    private String email;
    private String currentLevel;
    private String fitnessGoal;
    private String workoutSplit;
    private Integer lastWorkoutDay;
    private Long credentialsId;

    public CreateProfileRequest() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(String currentLevel) { this.currentLevel = currentLevel; }

    public String getFitnessGoal() { return fitnessGoal; }
    public void setFitnessGoal(String fitnessGoal) { this.fitnessGoal = fitnessGoal; }

    public String getWorkoutSplit() { return workoutSplit; }
    public void setWorkoutSplit(String workoutSplit) { this.workoutSplit = workoutSplit; }

    public Integer getLastWorkoutDay() { return lastWorkoutDay; }
    public void setLastWorkoutDay(Integer lastWorkoutDay) { this.lastWorkoutDay = lastWorkoutDay; }

    public Long getCredentialsId() { return credentialsId; }
    public void setCredentialsId(Long credentialsId) { this.credentialsId = credentialsId; }
}

