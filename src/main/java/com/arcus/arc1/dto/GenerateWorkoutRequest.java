package com.arcus.arc1.dto;

import java.util.List;

public class GenerateWorkoutRequest {

    private Long userId;
    private String level;
    private String goal;
    private String split;
    private Integer lastWorkoutDay;
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

    public Integer getLastWorkoutDay() { return lastWorkoutDay; }
    public void setLastWorkoutDay(Integer lastWorkoutDay) { this.lastWorkoutDay = lastWorkoutDay; }

    public List<String> getRequestedMuscles() { return requestedMuscles; }
    public void setRequestedMuscles(List<String> requestedMuscles) { this.requestedMuscles = requestedMuscles; }
}

