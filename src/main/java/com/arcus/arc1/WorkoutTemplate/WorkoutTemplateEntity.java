package com.arcus.arc1.WorkoutTemplate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.List;

@Entity
public class WorkoutTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String level;
    private String goal;
    private String split;
    private Integer dayNumber;
    private String muscleGroup;

     public WorkoutTemplateEntity() {}

     public WorkoutTemplateEntity(String name, String level, String goal, Integer dayNumber, String muscleGroups) {
         this.name = name;
         this.level = level;
         this.goal = goal;
         this.dayNumber = dayNumber;
         this.muscleGroup = muscleGroups;
     }

     public String getMuscleGroups() {
         return muscleGroup;
     }

     public void setMuscleGroups(String muscleGroups) {
         this.muscleGroup = muscleGroups;
     }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLevel() {
        return level;
    }

    public String getGoal() {
        return goal;
    }

    public Integer getDayNumber() {
        return dayNumber;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public String getSplit() {
        return split;
    }

    public void setSplit(String split) {
        this.split = split;
    }

    public void setDayNumber(Integer dayNumber) {
        this.dayNumber = dayNumber;
    }
}
