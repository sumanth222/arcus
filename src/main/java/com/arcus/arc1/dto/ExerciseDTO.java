package com.arcus.arc1.dto;

public class ExerciseDTO {

    private Long exerciseSessionId;
    private String exerciseName;
    private Double targetWeight;
    private Integer repMin;
    private Integer repMax;
    private Integer sets;
    private String tempo;

    public ExerciseDTO() {
    }

    public ExerciseDTO(Long exerciseSessionId, String exerciseName, Double targetWeight, Integer repMin, Integer repMax, Integer sets, String tempo) {
        this.exerciseSessionId = exerciseSessionId;
        this.exerciseName = exerciseName;
        this.targetWeight = targetWeight;
        this.repMin = repMin;
        this.repMax = repMax;
        this.sets = sets;
        this.tempo = tempo;
    }

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
}