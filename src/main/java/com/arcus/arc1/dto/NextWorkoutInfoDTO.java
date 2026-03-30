package com.arcus.arc1.dto;

import java.time.LocalDateTime;
import java.util.List;

public class NextWorkoutInfoDTO {

    private String nextWorkoutName;
    private Integer nextDayNumber;
    private String lastWorkoutName;
    private Integer lastDayNumber;
    private LocalDateTime lastWorkoutDate;
    private boolean lastWorkoutCompleted;
    private Double lastWorkoutTotalWeight;
    private Double previousWorkoutTotalWeight;
    private Double lastWorkoutWeightChange;
    private Double lastWorkoutWeightChangePercent;
    private List<String> muscleGroups;

    public NextWorkoutInfoDTO() {}

    public NextWorkoutInfoDTO(String nextWorkoutName, Integer nextDayNumber,
                              String lastWorkoutName, Integer lastDayNumber,
                              LocalDateTime lastWorkoutDate, boolean lastWorkoutCompleted,
                              Double lastWorkoutTotalWeight, Double previousWorkoutTotalWeight,
                              Double lastWorkoutWeightChange, Double lastWorkoutWeightChangePercent,
                              List<String> muscleGroups) {
        this.nextWorkoutName = nextWorkoutName;
        this.nextDayNumber = nextDayNumber;
        this.lastWorkoutName = lastWorkoutName;
        this.lastDayNumber = lastDayNumber;
        this.lastWorkoutDate = lastWorkoutDate;
        this.lastWorkoutCompleted = lastWorkoutCompleted;
        this.lastWorkoutTotalWeight = lastWorkoutTotalWeight;
        this.previousWorkoutTotalWeight = previousWorkoutTotalWeight;
        this.lastWorkoutWeightChange = lastWorkoutWeightChange;
        this.lastWorkoutWeightChangePercent = lastWorkoutWeightChangePercent;
        this.muscleGroups = muscleGroups;
    }

    public String getNextWorkoutName() { return nextWorkoutName; }
    public void setNextWorkoutName(String nextWorkoutName) { this.nextWorkoutName = nextWorkoutName; }

    public Integer getNextDayNumber() { return nextDayNumber; }
    public void setNextDayNumber(Integer nextDayNumber) { this.nextDayNumber = nextDayNumber; }

    public String getLastWorkoutName() { return lastWorkoutName; }
    public void setLastWorkoutName(String lastWorkoutName) { this.lastWorkoutName = lastWorkoutName; }

    public Integer getLastDayNumber() { return lastDayNumber; }
    public void setLastDayNumber(Integer lastDayNumber) { this.lastDayNumber = lastDayNumber; }

    public LocalDateTime getLastWorkoutDate() { return lastWorkoutDate; }
    public void setLastWorkoutDate(LocalDateTime lastWorkoutDate) { this.lastWorkoutDate = lastWorkoutDate; }

    public boolean isLastWorkoutCompleted() { return lastWorkoutCompleted; }
    public void setLastWorkoutCompleted(boolean lastWorkoutCompleted) { this.lastWorkoutCompleted = lastWorkoutCompleted; }

    public Double getLastWorkoutTotalWeight() { return lastWorkoutTotalWeight; }
    public void setLastWorkoutTotalWeight(Double lastWorkoutTotalWeight) { this.lastWorkoutTotalWeight = lastWorkoutTotalWeight; }

    public Double getPreviousWorkoutTotalWeight() { return previousWorkoutTotalWeight; }
    public void setPreviousWorkoutTotalWeight(Double previousWorkoutTotalWeight) { this.previousWorkoutTotalWeight = previousWorkoutTotalWeight; }

    public Double getLastWorkoutWeightChange() { return lastWorkoutWeightChange; }
    public void setLastWorkoutWeightChange(Double lastWorkoutWeightChange) { this.lastWorkoutWeightChange = lastWorkoutWeightChange; }

    public Double getLastWorkoutWeightChangePercent() { return lastWorkoutWeightChangePercent; }
    public void setLastWorkoutWeightChangePercent(Double lastWorkoutWeightChangePercent) { this.lastWorkoutWeightChangePercent = lastWorkoutWeightChangePercent; }

    public List<String> getMuscleGroups() {
        return muscleGroups;
    }

    public void setMuscleGroups(List<String> muscleGroups) {
        this.muscleGroups = muscleGroups;
    }
}
