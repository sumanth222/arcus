package com.arcus.arc1.ExerciseSession;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class ExerciseSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long workoutSessionId;

    /** FK to exercise_library — used for substitution and muscle-group tracking */
    private Long exerciseLibraryId;

    private String exerciseName;
    private Double targetWeight;
    private Integer repMin;
    private Integer repMax;
    private Integer sets;
    private String tempo;
    private Integer completedSets;
    private Boolean isCompleted;

    private Long workoutExerciseTemplateId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWorkoutSessionId() {
        return workoutSessionId;
    }

    public void setWorkoutSessionId(Long workoutSessionId) {
        this.workoutSessionId = workoutSessionId;
    }

    public Long getExerciseLibraryId() {
        return exerciseLibraryId;
    }

    public void setExerciseLibraryId(Long exerciseLibraryId) {
        this.exerciseLibraryId = exerciseLibraryId;
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

    public Integer getCompletedSets() {
        return completedSets;
    }

    public void setCompletedSets(Integer completedSets) {
        this.completedSets = completedSets;
    }

    public Boolean getIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(Boolean isCompleted) {
        this.isCompleted = isCompleted;
    }

    public Long getWorkoutExerciseTemplateId() {
        return workoutExerciseTemplateId;
    }

    public void setWorkoutExerciseTemplateId(Long workoutExerciseTemplateId) {
        this.workoutExerciseTemplateId = workoutExerciseTemplateId;
    }
}
