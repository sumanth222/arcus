package com.arcus.arc1.dto;

import java.time.LocalDateTime;

/**
 * DTO for User Profile response.
 * Contains all user profile information for API responses.
 */
public class UserProfileDTO {

    private Long userId;
    private String name;
    private String email;
    private LocalDateTime joinedAt;
    private String currentLevel;
    private String fitnessGoal;
    private String bio;

    // Workout statistics
    private Integer totalWorkouts;
    private Integer totalExerciseSessions;
    private Integer totalSetsSessions;
    private Double totalWeightLifted;

    // Engagement
    private Integer consecutiveWorkoutDays;
    private LocalDateTime lastWorkoutDate;

    private Integer lastWorkoutDay;
    private Integer lastExerciseSessionId;
    private String workoutSplit;

    // Constructor
    public UserProfileDTO() {}

    public UserProfileDTO(Long userId, String name, String email, LocalDateTime joinedAt,
                         String currentLevel, String fitnessGoal, String bio,
                         Integer totalWorkouts, Integer totalExerciseSessions,
                         Integer totalSetsSessions, Double totalWeightLifted,
                         Integer consecutiveWorkoutDays, LocalDateTime lastWorkoutDate, Integer lastWorkoutDay,
                          Integer lastExerciseSessionId) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.joinedAt = joinedAt;
        this.currentLevel = currentLevel;
        this.fitnessGoal = fitnessGoal;
        this.bio = bio;
        this.totalWorkouts = totalWorkouts;
        this.totalExerciseSessions = totalExerciseSessions;
        this.totalSetsSessions = totalSetsSessions;
        this.totalWeightLifted = totalWeightLifted;
        this.consecutiveWorkoutDays = consecutiveWorkoutDays;
        this.lastWorkoutDate = lastWorkoutDate;
        this.lastWorkoutDay = lastWorkoutDay;
        this.lastExerciseSessionId = lastExerciseSessionId;
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public String getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(String currentLevel) {
        this.currentLevel = currentLevel;
    }

    public String getFitnessGoal() {
        return fitnessGoal;
    }

    public void setFitnessGoal(String fitnessGoal) {
        this.fitnessGoal = fitnessGoal;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Integer getTotalWorkouts() {
        return totalWorkouts;
    }

    public void setTotalWorkouts(Integer totalWorkouts) {
        this.totalWorkouts = totalWorkouts;
    }

    public Integer getTotalExerciseSessions() {
        return totalExerciseSessions;
    }

    public void setTotalExerciseSessions(Integer totalExerciseSessions) {
        this.totalExerciseSessions = totalExerciseSessions;
    }

    public Integer getTotalSetsSessions() {
        return totalSetsSessions;
    }

    public void setTotalSetsSessions(Integer totalSetsSessions) {
        this.totalSetsSessions = totalSetsSessions;
    }

    public Double getTotalWeightLifted() {
        return totalWeightLifted;
    }

    public void setTotalWeightLifted(Double totalWeightLifted) {
        this.totalWeightLifted = totalWeightLifted;
    }

    public Integer getConsecutiveWorkoutDays() {
        return consecutiveWorkoutDays;
    }

    public void setConsecutiveWorkoutDays(Integer consecutiveWorkoutDays) {
        this.consecutiveWorkoutDays = consecutiveWorkoutDays;
    }

    public LocalDateTime getLastWorkoutDate() {
        return lastWorkoutDate;
    }

    public void setLastWorkoutDate(LocalDateTime lastWorkoutDate) {
        this.lastWorkoutDate = lastWorkoutDate;
    }

    public Integer getLastWorkoutDay() {
        return lastWorkoutDay;
    }

    public void setLastWorkoutDay(Integer lastWorkoutDay) {
        this.lastWorkoutDay = lastWorkoutDay;
    }

    public Integer getLastExerciseSessionId() {
        return lastExerciseSessionId;
    }

    public void setLastExerciseSessionId(Integer lastExerciseSessionId) {
        this.lastExerciseSessionId = lastExerciseSessionId;
    }

    public String getWorkoutSplit() {
        return workoutSplit;
    }

    public void setWorkoutSplit(String workoutSplit) {
        this.workoutSplit = workoutSplit;
    }
}

