package com.arcus.arc1.dto;

import java.util.List;

public class GenerateWorkoutRequest {

    private Long userId;
    private String level;
    private String goal;
    private String split;
    private Integer dayNumber;
    private List<String> requestedMuscles;

    public GenerateWorkoutRequest() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public String getSplit() { return split; }
    public void setSplit(String split) { this.split = split; }

    public Integer getDayNumber() { return dayNumber; }
    public void setDayNumber(Integer dayNumber) { this.dayNumber = dayNumber; }

    public List<String> getRequestedMuscles() { return requestedMuscles; }
    public void setRequestedMuscles(List<String> requestedMuscles) { this.requestedMuscles = requestedMuscles; }
}

