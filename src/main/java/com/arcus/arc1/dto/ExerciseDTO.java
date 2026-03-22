package com.arcus.arc1.dto;

public class ExerciseDTO {

    private String exerciseName;
    private Double targetWeight;
    private Integer repMin;
    private Integer repMax;
    private Integer sets;
    private String tempo;
    private String tip;
    private String muscleArea; // Optional: for future use
    private String secondaryMuscleGroup;
    private Long exerciseTemplateSessionID;

    public ExerciseDTO() {
    }


    public ExerciseDTO(String exerciseName,
                       Double targetWeight, Integer repMin, Integer repMax,
                       Integer sets, String tempo, String tip, String muscleArea,
                       String secondaryMuscleGroup, int exerciseTemplateSessionID) {
        this.exerciseName = exerciseName;
        this.targetWeight = targetWeight;
        this.repMin = repMin;
        this.repMax = repMax;
        this.sets = sets;
        this.tempo = tempo;
        this.tip = tip;
        this.muscleArea = muscleArea;
        this.secondaryMuscleGroup = secondaryMuscleGroup;
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

    public String getTip() {
        return tip;
    }

    public void setTip(String tip) {
        this.tip = tip;
    }

    public String getMuscleArea() {
        return muscleArea;
    }

    public void setMuscleArea(String muscleArea) {
        this.muscleArea = muscleArea;
    }

    public String getSecondaryMuscleGroup() {
        return secondaryMuscleGroup;
    }

    public void setSecondaryMuscleGroup(String secondaryMuscleGroup) {
        this.secondaryMuscleGroup = secondaryMuscleGroup;
    }

    public Long getExerciseTemplateSessionID() {
        return exerciseTemplateSessionID;
    }

    public void setExerciseTemplateSessionID(Long exerciseTemplateSessionID) {
        this.exerciseTemplateSessionID = exerciseTemplateSessionID;
    }
}