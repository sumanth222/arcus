package com.arcus.arc1.ExerciseLibrary;

import jakarta.persistence.*;

/**
 * Master catalog of all exercises.
 *
 * This is the single source of truth for exercise metadata.
 * Templates and sessions reference exercises by ID from this table,
 * enabling muscle-group-based substitution and dynamic workout generation.
 *
 * equipment values  : barbell | dumbbell | cable | machine | bodyweight | smith_machine | ez_bar
 * muscleGroup values: chest | back | shoulders | quads | hamstrings | glutes | biceps | triceps | core
 * category values   : compound | isolation
 */
@Entity
@Table(name = "exercise_library")
public class ExerciseLibraryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    /** Primary muscle group targeted */
    @Column(nullable = false)
    private String muscleGroup;

    /** Comma-separated secondary muscles, e.g. "triceps,shoulders" */
    private String secondaryMuscles;

    /** Optional finer-grained muscle area/region, e.g. "upper", "lower", "short_head", "long_head" */
    private String muscleArea;

    /** Difficulty/level for the exercise: e.g. "beginner", "intermediate", "advanced" or "all" */
    private String level;

    /** Equipment required: barbell, dumbbell, cable, machine, bodyweight, smith_machine, ez_bar */
    @Column(nullable = false)
    private String equipment;

    /** compound or isolation */
    @Column(nullable = false)
    private String category;

    /** Short coaching tip shown in the app */
    @Column(length = 500)
    private String tip;

    @Column
    private int repMin;

    @Column
    private int repMax;

    @Column
    private int sets;

    public ExerciseLibraryEntity() {}

    public ExerciseLibraryEntity(Long id, String name, String muscleGroup, String secondaryMuscles, String muscleArea,
                                 String level, String equipment, String category, String tip, int repMin, int repMax,
                                 int sets) {
        this.id = id;
        this.name = name;
        this.muscleGroup = muscleGroup;
        this.secondaryMuscles = secondaryMuscles;
        this.muscleArea = muscleArea;
        this.level = level;
        this.equipment = equipment;
        this.category = category;
        this.tip = tip;
        this.repMin = repMin;
        this.repMax = repMax;
        this.sets = sets;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMuscleGroup() { return muscleGroup; }
    public void setMuscleGroup(String muscleGroup) { this.muscleGroup = muscleGroup; }

    public String getSecondaryMuscles() { return secondaryMuscles; }
    public void setSecondaryMuscles(String secondaryMuscles) { this.secondaryMuscles = secondaryMuscles; }

    public String getMuscleArea() { return muscleArea; }
    public void setMuscleArea(String muscleArea) { this.muscleArea = muscleArea; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getEquipment() { return equipment; }
    public void setEquipment(String equipment) { this.equipment = equipment; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTip() { return tip; }
    public void setTip(String tip) { this.tip = tip; }

    public int getRepMin() {
        return repMin;
    }

    public void setRepMin(int repMin) {
        this.repMin = repMin;
    }

    public int getRepMax() {
        return repMax;
    }

    public void setRepMax(int repMax) {
        this.repMax = repMax;
    }

    public int getSets() {
        return sets;
    }

    public void setSets(int sets) {
        this.sets = sets;
    }
}




