package com.arcus.arc1.dto;

public class ExerciseSubstituteDTO {

    private Long libraryId;
    private String name;
    private String muscleGroup;
    private String secondaryMuscles;
    private String equipment;
    private String category;
    private String tip;

    public ExerciseSubstituteDTO() {}

    public ExerciseSubstituteDTO(Long libraryId, String name, String muscleGroup,
                                  String secondaryMuscles, String equipment,
                                  String category, String tip) {
        this.libraryId = libraryId;
        this.name = name;
        this.muscleGroup = muscleGroup;
        this.secondaryMuscles = secondaryMuscles;
        this.equipment = equipment;
        this.category = category;
        this.tip = tip;
    }

    public Long getLibraryId() { return libraryId; }
    public void setLibraryId(Long libraryId) { this.libraryId = libraryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMuscleGroup() { return muscleGroup; }
    public void setMuscleGroup(String muscleGroup) { this.muscleGroup = muscleGroup; }

    public String getSecondaryMuscles() { return secondaryMuscles; }
    public void setSecondaryMuscles(String secondaryMuscles) { this.secondaryMuscles = secondaryMuscles; }

    public String getEquipment() { return equipment; }
    public void setEquipment(String equipment) { this.equipment = equipment; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTip() { return tip; }
    public void setTip(String tip) { this.tip = tip; }
}

