package com.arcus.arc1.dto;

import java.util.List;

public class WorkoutResponseDTO {

    private Long sessionId;
    private String level;
    private List<ExerciseDTO> exercises;

    public WorkoutResponseDTO() {
    }

    public WorkoutResponseDTO(Long sessionId, String level, List<ExerciseDTO> exercises) {
        this.sessionId = sessionId;
        this.level = level;
        this.exercises = exercises;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public List<ExerciseDTO> getExercises() {
        return exercises;
    }

    public void setExercises(List<ExerciseDTO> exercises) {
        this.exercises = exercises;
    }
}