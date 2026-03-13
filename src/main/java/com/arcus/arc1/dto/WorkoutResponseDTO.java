package com.arcus.arc1.dto;

import java.util.List;

public class WorkoutResponseDTO {

    private Long sessionId;
    private String level;
    private Integer dayNumber;
    private List<ExerciseDTO> exercises;

    public WorkoutResponseDTO() {
    }

    public WorkoutResponseDTO(Long sessionId, String level, Integer dayNumber, List<ExerciseDTO> exercises) {
        this.sessionId = sessionId;
        this.level = level;
        this.dayNumber = dayNumber;
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

    public Integer getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(Integer dayNumber) {
        this.dayNumber = dayNumber;
    }

    public List<ExerciseDTO> getExercises() {
        return exercises;
    }

    public void setExercises(List<ExerciseDTO> exercises) {
        this.exercises = exercises;
    }
}