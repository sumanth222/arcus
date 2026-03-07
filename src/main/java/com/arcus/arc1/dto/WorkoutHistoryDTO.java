package com.arcus.arc1.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for detailed workout history item.
 * Used for displaying workout information in charts and UI.
 */
public class WorkoutHistoryDTO {

    private Long workoutSessionId;
    private LocalDateTime createdAt;
    private boolean completed;
    private Integer totalExercises;
    private Integer totalSets;
    private Double totalWeightLifted;
    private List<ExerciseHistoryDTO> exercises;

    // Constructor
    public WorkoutHistoryDTO() {}

    public WorkoutHistoryDTO(Long workoutSessionId, LocalDateTime createdAt, boolean completed,
                            Integer totalExercises, Integer totalSets, Double totalWeightLifted) {
        this.workoutSessionId = workoutSessionId;
        this.createdAt = createdAt;
        this.completed = completed;
        this.totalExercises = totalExercises;
        this.totalSets = totalSets;
        this.totalWeightLifted = totalWeightLifted;
    }

    // Getters and Setters
    public Long getWorkoutSessionId() {
        return workoutSessionId;
    }

    public void setWorkoutSessionId(Long workoutSessionId) {
        this.workoutSessionId = workoutSessionId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Integer getTotalExercises() {
        return totalExercises;
    }

    public void setTotalExercises(Integer totalExercises) {
        this.totalExercises = totalExercises;
    }

    public Integer getTotalSets() {
        return totalSets;
    }

    public void setTotalSets(Integer totalSets) {
        this.totalSets = totalSets;
    }

    public Double getTotalWeightLifted() {
        return totalWeightLifted;
    }

    public void setTotalWeightLifted(Double totalWeightLifted) {
        this.totalWeightLifted = totalWeightLifted;
    }

    public List<ExerciseHistoryDTO> getExercises() {
        return exercises;
    }

    public void setExercises(List<ExerciseHistoryDTO> exercises) {
        this.exercises = exercises;
    }
}

