package com.arcus.arc1.dto;

public class SetLogDTO {
    private Long exerciseSessionId;
    private Integer setNumber;
    private Double weight;
    private Integer reps;

    public SetLogDTO(Long exerciseSessionId, Integer setNumber, Double weight, Integer reps) {
        this.exerciseSessionId = exerciseSessionId;
        this.setNumber = setNumber;
        this.weight = weight;
        this.reps = reps;
    }

    public Long getExerciseSessionId() {
        return exerciseSessionId;
    }

    public void setExerciseSessionId(Long exerciseSessionId) {
        this.exerciseSessionId = exerciseSessionId;
    }

    public Integer getSetNumber() {
        return setNumber;
    }

    public void setSetNumber(Integer setNumber) {
        this.setNumber = setNumber;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Integer getReps() {
        return reps;
    }

    public void setReps(Integer reps) {
        this.reps = reps;
    }
}