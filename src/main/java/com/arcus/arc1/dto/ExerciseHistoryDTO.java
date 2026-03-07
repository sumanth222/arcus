package com.arcus.arc1.dto;

import java.util.List;

/**
 * DTO for exercise details within a workout history.
 * Shows exercise name, weight, reps, and sets performed.
 */
public class ExerciseHistoryDTO {

    private Long exerciseSessionId;
    private String exerciseName;
    private Double targetWeight;
    private Integer repMin;
    private Integer repMax;
    private Integer sets;
    private String tempo;
    private Integer setsCompleted;
    private Double averageReps;
    private Double totalWeightForExercise;
    private List<SetHistoryDTO> setLogs;

    // Constructor
    public ExerciseHistoryDTO() {}

    public ExerciseHistoryDTO(Long exerciseSessionId, String exerciseName, Double targetWeight,
                             Integer repMin, Integer repMax, Integer sets, String tempo,
                             Integer setsCompleted, Double averageReps, Double totalWeightForExercise) {
        this.exerciseSessionId = exerciseSessionId;
        this.exerciseName = exerciseName;
        this.targetWeight = targetWeight;
        this.repMin = repMin;
        this.repMax = repMax;
        this.sets = sets;
        this.tempo = tempo;
        this.setsCompleted = setsCompleted;
        this.averageReps = averageReps;
        this.totalWeightForExercise = totalWeightForExercise;
    }

    // Getters and Setters
    public Long getExerciseSessionId() {
        return exerciseSessionId;
    }

    public void setExerciseSessionId(Long exerciseSessionId) {
        this.exerciseSessionId = exerciseSessionId;
    }

    public String getExerciseName() {
        return exerciseName;
    }

    public void setExerciseName(String exerciseName) {
        this.exerciseName = exerciseName;
    }

    public Double getTargetWeight() {
        return targetWeight;
    }

    public void setTargetWeight(Double targetWeight) {
        this.targetWeight = targetWeight;
    }

    public Integer getRepMin() {
        return repMin;
    }

    public void setRepMin(Integer repMin) {
        this.repMin = repMin;
    }

    public Integer getRepMax() {
        return repMax;
    }

    public void setRepMax(Integer repMax) {
        this.repMax = repMax;
    }

    public Integer getSets() {
        return sets;
    }

    public void setSets(Integer sets) {
        this.sets = sets;
    }

    public String getTempo() {
        return tempo;
    }

    public void setTempo(String tempo) {
        this.tempo = tempo;
    }

    public Integer getSetsCompleted() {
        return setsCompleted;
    }

    public void setSetsCompleted(Integer setsCompleted) {
        this.setsCompleted = setsCompleted;
    }

    public Double getAverageReps() {
        return averageReps;
    }

    public void setAverageReps(Double averageReps) {
        this.averageReps = averageReps;
    }

    public Double getTotalWeightForExercise() {
        return totalWeightForExercise;
    }

    public void setTotalWeightForExercise(Double totalWeightForExercise) {
        this.totalWeightForExercise = totalWeightForExercise;
    }

    public List<SetHistoryDTO> getSetLogs() {
        return setLogs;
    }

    public void setSetLogs(List<SetHistoryDTO> setLogs) {
        this.setLogs = setLogs;
    }
}

