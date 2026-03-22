package com.arcus.arc1.dto;

import java.util.List;

public class MuscleRequest {

    private String muscleGroup;         // e.g. "chest", "triceps"
    private List<String> areasOrder;    // e.g. ["upper","middle","lower"] — order to pick from
    private Integer count;              // how many exercises to pick in total for this muscleGroup

    public MuscleRequest() {}

    public String getMuscleGroup() { return muscleGroup; }
    public void setMuscleGroup(String muscleGroup) { this.muscleGroup = muscleGroup; }

    public List<String> getAreasOrder() { return areasOrder; }
    public void setAreasOrder(List<String> areasOrder) { this.areasOrder = areasOrder; }

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
}

