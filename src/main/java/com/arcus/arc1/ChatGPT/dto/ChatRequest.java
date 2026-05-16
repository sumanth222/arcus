package com.arcus.arc1.ChatGPT.dto;

public class ChatRequest {
    private String message;
    private String context; // optional system context (e.g. "You are a fitness coach")
    private String exerciseName;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }

    public String getExerciseName() { return exerciseName; }
    public void setExerciseName(String exerciseName) { this.exerciseName = exerciseName; }
}
