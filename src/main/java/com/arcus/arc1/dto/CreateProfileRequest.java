package com.arcus.arc1.dto;

public class CreateProfileRequest {

    private Long userId;
    private String username;  // used to link UserCredentials if credentialsId not provided
    private String name;
    private String email;
    private String currentLevel;
    private String fitnessGoal;
    private String workoutSplit;
    private String workoutLocation;
    private Integer lastWorkoutDay;
    private Long credentialsId;
    private Double weightKg;
    private Double heightCm;

    // New onboarding fields (all optional)
    private Integer age;
    private String gender;
    private Integer daysPerWeek;
    private Integer workoutDuration;
    private String equipment;
    private String weakMuscleGroups;
    private String injuries;
    private String additionalNotes;

    public CreateProfileRequest() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(String currentLevel) { this.currentLevel = currentLevel; }

    public String getFitnessGoal() { return fitnessGoal; }
    public void setFitnessGoal(String fitnessGoal) { this.fitnessGoal = fitnessGoal; }

    public String getWorkoutSplit() { return workoutSplit; }
    public void setWorkoutSplit(String workoutSplit) { this.workoutSplit = workoutSplit; }

    public String getWorkoutLocation() { return workoutLocation; }
    public void setWorkoutLocation(String workoutLocation) { this.workoutLocation = workoutLocation; }

    public Integer getLastWorkoutDay() { return lastWorkoutDay; }
    public void setLastWorkoutDay(Integer lastWorkoutDay) { this.lastWorkoutDay = lastWorkoutDay; }

    public Long getCredentialsId() { return credentialsId; }
    public void setCredentialsId(Long credentialsId) { this.credentialsId = credentialsId; }

    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }

    public Double getHeightCm() { return heightCm; }
    public void setHeightCm(Double heightCm) { this.heightCm = heightCm; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Integer getDaysPerWeek() { return daysPerWeek; }
    public void setDaysPerWeek(Integer daysPerWeek) { this.daysPerWeek = daysPerWeek; }

    public Integer getWorkoutDuration() { return workoutDuration; }
    public void setWorkoutDuration(Integer workoutDuration) { this.workoutDuration = workoutDuration; }

    public String getEquipment() { return equipment; }
    public void setEquipment(String equipment) { this.equipment = equipment; }

    public String getWeakMuscleGroups() { return weakMuscleGroups; }
    public void setWeakMuscleGroups(String weakMuscleGroups) { this.weakMuscleGroups = weakMuscleGroups; }

    public String getInjuries() { return injuries; }
    public void setInjuries(String injuries) { this.injuries = injuries; }

    public String getAdditionalNotes() { return additionalNotes; }
    public void setAdditionalNotes(String additionalNotes) { this.additionalNotes = additionalNotes; }
}
