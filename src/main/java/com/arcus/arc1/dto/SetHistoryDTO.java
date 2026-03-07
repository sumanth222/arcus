package com.arcus.arc1.dto;

/**
 * DTO for individual set details within exercise history.
 * Shows weight, reps, and set number.
 */
public class SetHistoryDTO {

    private Long setLogId;
    private Integer setNumber;
    private Double weight;
    private Integer reps;

    // Constructor
    public SetHistoryDTO() {}

    public SetHistoryDTO(Long setLogId, Integer setNumber, Double weight, Integer reps) {
        this.setLogId = setLogId;
        this.setNumber = setNumber;
        this.weight = weight;
        this.reps = reps;
    }

    // Getters and Setters
    public Long getSetLogId() {
        return setLogId;
    }

    public void setSetLogId(Long setLogId) {
        this.setLogId = setLogId;
    }

    public Integer getSetNumber() {
        return setNumber;
    }

    public void setSetNumber(Integer setNumber) {
        this.setNumber = setNumber;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Integer getReps() {
        return reps;
    }

    public void setReps(Integer reps) {
        this.reps = reps;
    }
}

