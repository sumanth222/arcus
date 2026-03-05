package com.arcus.arc1.dto;

/**
 * Response DTO for set evaluation.
 * Provides feedback after a set is logged, including fatigue detection and rest recommendations.
 */
public class SetEvaluationDTO {
    private Boolean fatigueDetected;
    private Integer suggestedRestSeconds;
    private String message;
    private Boolean exerciseCompleted;

    public SetEvaluationDTO(Boolean fatigueDetected, Integer suggestedRestSeconds, String message) {
        this.fatigueDetected = fatigueDetected;
        this.suggestedRestSeconds = suggestedRestSeconds;
        this.message = message;
        this.exerciseCompleted = false;
    }

    public SetEvaluationDTO(Boolean fatigueDetected, Integer suggestedRestSeconds, String message, Boolean exerciseCompleted) {
        this.fatigueDetected = fatigueDetected;
        this.suggestedRestSeconds = suggestedRestSeconds;
        this.message = message;
        this.exerciseCompleted = exerciseCompleted;
    }

    public Boolean getFatigueDetected() {
        return fatigueDetected;
    }

    public void setFatigueDetected(Boolean fatigueDetected) {
        this.fatigueDetected = fatigueDetected;
    }

    public Integer getSuggestedRestSeconds() {
        return suggestedRestSeconds;
    }

    public void setSuggestedRestSeconds(Integer suggestedRestSeconds) {
        this.suggestedRestSeconds = suggestedRestSeconds;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getExerciseCompleted() {
        return exerciseCompleted;
    }

    public void setExerciseCompleted(Boolean exerciseCompleted) {
        this.exerciseCompleted = exerciseCompleted;
    }
}

