package com.arcus.arc1.dto;

import java.util.List;

public class ReplaceExerciseRequest {

    private Long exerciseSessionId;   // the template row being replaced (will be voided)
    private Long userId;
    private String level;             // optional — fetched from profile if blank
    private String goal;              // optional — fetched from profile if blank
    private String muscleGroup;       // replacement must target this muscle group
    private List<Long> excludeExerciseIds; // all library IDs already in the session

    public ReplaceExerciseRequest() {}

    public Long getExerciseSessionId() { return exerciseSessionId; }
    public void setExerciseSessionId(Long exerciseSessionId) { this.exerciseSessionId = exerciseSessionId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public String getMuscleGroup() { return muscleGroup; }
    public void setMuscleGroup(String muscleGroup) { this.muscleGroup = muscleGroup; }

    public List<Long> getExcludeExerciseIds() { return excludeExerciseIds; }
    public void setExcludeExerciseIds(List<Long> excludeExerciseIds) { this.excludeExerciseIds = excludeExerciseIds; }
}

