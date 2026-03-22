package com.arcus.arc1.dto;

public class SetLogDTO {
    private Long exerciseSessionId;
    private Long workoutExerciseTemplateId;
    private Integer setNumber;
    private Double weight;
    private Integer reps;

    public SetLogDTO(Long exerciseSessionId, Long workoutExerciseTemplateId, Integer setNumber, Double weight, Integer reps) {
        this.exerciseSessionId = exerciseSessionId;
        this.workoutExerciseTemplateId = workoutExerciseTemplateId;
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

    public Long getWorkoutExerciseTemplateId() {
        return workoutExerciseTemplateId;
    }

    public void setWorkoutExerciseTemplateId(Long workoutExerciseTemplateId) {
        this.workoutExerciseTemplateId = workoutExerciseTemplateId;
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