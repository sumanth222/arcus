package com.arcus.arc1.dto;

import java.time.LocalDateTime;

public class NextWorkoutInfoDTO {

    private String nextWorkoutName;
    private Integer nextDayNumber;
    private String lastWorkoutName;
    private Integer lastDayNumber;
    private LocalDateTime lastWorkoutDate;
    private boolean lastWorkoutCompleted;
    private Integer lastWorkoutTotalWeight;

    public NextWorkoutInfoDTO() {}

    public NextWorkoutInfoDTO(String nextWorkoutName, Integer nextDayNumber,
                              String lastWorkoutName, Integer lastDayNumber,
                              LocalDateTime lastWorkoutDate, boolean lastWorkoutCompleted,
                              Integer lastWorkoutTotalWeight) {
        this.nextWorkoutName = nextWorkoutName;
        this.nextDayNumber = nextDayNumber;
        this.lastWorkoutName = lastWorkoutName;
        this.lastDayNumber = lastDayNumber;
        this.lastWorkoutDate = lastWorkoutDate;
        this.lastWorkoutCompleted = lastWorkoutCompleted;
        this.lastWorkoutTotalWeight = lastWorkoutTotalWeight;
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

    public Integer getLastWorkoutTotalWeight() { return lastWorkoutTotalWeight; }
    public void setLastWorkoutTotalWeight(Integer lastWorkoutTotalWeight) { this.lastWorkoutTotalWeight = lastWorkoutTotalWeight; }
}

