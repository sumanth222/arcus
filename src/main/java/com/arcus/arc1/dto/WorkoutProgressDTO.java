package com.arcus.arc1.dto;

/**
 * DTO for workout session progress.
 * Shows current state of an active workout session.
 * Perfect for displaying progress to the user during a workout.
 */
public class WorkoutProgressDTO {

    private Long workoutSessionId;
    private Long userId;
    private boolean workoutCompleted;

    // Current exercise info
    private Long currentExerciseSessionId;
    private String currentExerciseName;
    private Double currentExerciseWeight;
    private Integer currentExerciseRepMin;
    private Integer currentExerciseRepMax;
    private Integer currentExerciseSetsPlanned;

    // Progress info
    private Integer setsCompleted;
    private Integer setsRemaining;
    private Double setsCompletedPercentage;

    // Next exercise info
    private Long nextExerciseSessionId;
    private String nextExerciseName;
    private Double nextExerciseWeight;

    // Session info
    private String sessionStatus; // "in_progress", "completed", "not_started"
    private Integer totalExercises;
    private Integer completedExercises;

    // Constructor
    public WorkoutProgressDTO() {}

    public WorkoutProgressDTO(Long workoutSessionId, Long userId, boolean workoutCompleted,
                             Long currentExerciseSessionId, String currentExerciseName,
                             Double currentExerciseWeight, Integer currentExerciseRepMin,
                             Integer currentExerciseRepMax, Integer currentExerciseSetsPlanned,
                             Integer setsCompleted, Integer setsRemaining, Double setsCompletedPercentage,
                             Long nextExerciseSessionId, String nextExerciseName,
                             Double nextExerciseWeight, String sessionStatus,
                             Integer totalExercises, Integer completedExercises) {
        this.workoutSessionId = workoutSessionId;
        this.userId = userId;
        this.workoutCompleted = workoutCompleted;
        this.currentExerciseSessionId = currentExerciseSessionId;
        this.currentExerciseName = currentExerciseName;
        this.currentExerciseWeight = currentExerciseWeight;
        this.currentExerciseRepMin = currentExerciseRepMin;
        this.currentExerciseRepMax = currentExerciseRepMax;
        this.currentExerciseSetsPlanned = currentExerciseSetsPlanned;
        this.setsCompleted = setsCompleted;
        this.setsRemaining = setsRemaining;
        this.setsCompletedPercentage = setsCompletedPercentage;
        this.nextExerciseSessionId = nextExerciseSessionId;
        this.nextExerciseName = nextExerciseName;
        this.nextExerciseWeight = nextExerciseWeight;
        this.sessionStatus = sessionStatus;
        this.totalExercises = totalExercises;
        this.completedExercises = completedExercises;
    }

    // Getters and Setters
    public Long getWorkoutSessionId() {
        return workoutSessionId;
    }

    public void setWorkoutSessionId(Long workoutSessionId) {
        this.workoutSessionId = workoutSessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isWorkoutCompleted() {
        return workoutCompleted;
    }

    public void setWorkoutCompleted(boolean workoutCompleted) {
        this.workoutCompleted = workoutCompleted;
    }

    public Long getCurrentExerciseSessionId() {
        return currentExerciseSessionId;
    }

    public void setCurrentExerciseSessionId(Long currentExerciseSessionId) {
        this.currentExerciseSessionId = currentExerciseSessionId;
    }

    public String getCurrentExerciseName() {
        return currentExerciseName;
    }

    public void setCurrentExerciseName(String currentExerciseName) {
        this.currentExerciseName = currentExerciseName;
    }

    public Double getCurrentExerciseWeight() {
        return currentExerciseWeight;
    }

    public void setCurrentExerciseWeight(Double currentExerciseWeight) {
        this.currentExerciseWeight = currentExerciseWeight;
    }

    public Integer getCurrentExerciseRepMin() {
        return currentExerciseRepMin;
    }

    public void setCurrentExerciseRepMin(Integer currentExerciseRepMin) {
        this.currentExerciseRepMin = currentExerciseRepMin;
    }

    public Integer getCurrentExerciseRepMax() {
        return currentExerciseRepMax;
    }

    public void setCurrentExerciseRepMax(Integer currentExerciseRepMax) {
        this.currentExerciseRepMax = currentExerciseRepMax;
    }

    public Integer getCurrentExerciseSetsPlanned() {
        return currentExerciseSetsPlanned;
    }

    public void setCurrentExerciseSetsPlanned(Integer currentExerciseSetsPlanned) {
        this.currentExerciseSetsPlanned = currentExerciseSetsPlanned;
    }

    public Integer getSetsCompleted() {
        return setsCompleted;
    }

    public void setSetsCompleted(Integer setsCompleted) {
        this.setsCompleted = setsCompleted;
    }

    public Integer getSetsRemaining() {
        return setsRemaining;
    }

    public void setSetsRemaining(Integer setsRemaining) {
        this.setsRemaining = setsRemaining;
    }

    public Double getSetsCompletedPercentage() {
        return setsCompletedPercentage;
    }

    public void setSetsCompletedPercentage(Double setsCompletedPercentage) {
        this.setsCompletedPercentage = setsCompletedPercentage;
    }

    public Long getNextExerciseSessionId() {
        return nextExerciseSessionId;
    }

    public void setNextExerciseSessionId(Long nextExerciseSessionId) {
        this.nextExerciseSessionId = nextExerciseSessionId;
    }

    public String getNextExerciseName() {
        return nextExerciseName;
    }

    public void setNextExerciseName(String nextExerciseName) {
        this.nextExerciseName = nextExerciseName;
    }

    public Double getNextExerciseWeight() {
        return nextExerciseWeight;
    }

    public void setNextExerciseWeight(Double nextExerciseWeight) {
        this.nextExerciseWeight = nextExerciseWeight;
    }

    public String getSessionStatus() {
        return sessionStatus;
    }

    public void setSessionStatus(String sessionStatus) {
        this.sessionStatus = sessionStatus;
    }

    public Integer getTotalExercises() {
        return totalExercises;
    }

    public void setTotalExercises(Integer totalExercises) {
        this.totalExercises = totalExercises;
    }

    public Integer getCompletedExercises() {
        return completedExercises;
    }

    public void setCompletedExercises(Integer completedExercises) {
        this.completedExercises = completedExercises;
    }
}

