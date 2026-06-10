package com.arcus.arc1.UserProfile;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * User Profile Entity - Stores comprehensive user information.
 *
 * Tracks:
 * - Basic user info (ID, name, level)
 * - Account creation date
 * - Current fitness level
 * - Fitness goals
 * - Experience with workouts
 */
@Entity
@Table(name = "user_profile")
public class UserProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    @Column(nullable = false)
    private String currentLevel; // beginner, medium, advanced, expert

    @Column(nullable = false)
    private String fitnessGoal; // muscle_gain, strength, endurance, weight_loss

    @Column(columnDefinition = "TEXT")
    private String bio;

    // Workout statistics
    @Column(nullable = false)
    private Integer totalWorkouts = 0;

    @Column(nullable = false)
    private Integer totalExerciseSessions = 0;

    @Column(nullable = false)
    private Integer totalSetsSessions = 0;

    @Column(nullable = false)
    private Double totalWeightLifted = 0.0; // in kg

    // Engagement tracking
    @Column(nullable = false)
    private Integer consecutiveWorkoutDays = 0;

    @Column(nullable = true)
    private LocalDateTime lastWorkoutDate;

    @Column(nullable = false)
    private LocalDateTime lastUpdatedAt;

    @Column(nullable = true)
    private Integer lastWorkoutDay = 0;

    @Column(nullable = true)
    private Integer lastExerciseSessionId = 0;

    @Column(nullable = true)
    private String workoutSplit;
    private String workoutLocation;

    /** Body weight in kg — used for nutrition target calculations */
    @Column(nullable = true)
    private Double weightKg;

    /** Height in cm — used for BMR / calorie calculations */
    @Column(nullable = true)
    private Double heightCm;

    // --- New onboarding fields ---
    @Column(nullable = true)
    private Integer age;

    @Column(length = 50, nullable = true)
    private String gender;

    @Column(name = "days_per_week", nullable = true)
    private Integer daysPerWeek;

    @Column(name = "workout_duration", nullable = true)
    private Integer workoutDuration;

    @Column(length = 100, nullable = true)
    private String equipment;

    @Column(name = "weak_muscle_groups", columnDefinition = "TEXT", nullable = true)
    private String weakMuscleGroups;

    @Column(columnDefinition = "TEXT", nullable = true)
    private String injuries;

    @Column(name = "additional_notes", columnDefinition = "TEXT", nullable = true)
    private String additionalNotes;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private UserStatus status = UserStatus.PENDING_REVIEW;

    // Constructor
    public UserProfileEntity() {}

    public UserProfileEntity(Long userId, String name, String email, String currentLevel, String fitnessGoal, Integer lastWorkoutDay
    ,Integer LastExerciseSessionId) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.joinedAt = LocalDateTime.now();
        this.currentLevel = currentLevel;
        this.fitnessGoal = fitnessGoal;
        this.totalWorkouts = 0;
        this.totalExerciseSessions = 0;
        this.totalSetsSessions = 0;
        this.totalWeightLifted = 0.0;
        this.consecutiveWorkoutDays = 0;
        this.lastUpdatedAt = LocalDateTime.now();
        this.lastWorkoutDay = lastWorkoutDay;
        this.lastExerciseSessionId = LastExerciseSessionId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public String getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(String currentLevel) {
        this.currentLevel = currentLevel;
    }

    public String getFitnessGoal() {
        return fitnessGoal;
    }

    public void setFitnessGoal(String fitnessGoal) {
        this.fitnessGoal = fitnessGoal;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Integer getTotalWorkouts() {
        return totalWorkouts;
    }

    public void setTotalWorkouts(Integer totalWorkouts) {
        this.totalWorkouts = totalWorkouts;
    }

    public Integer getTotalExerciseSessions() {
        return totalExerciseSessions;
    }

    public void setTotalExerciseSessions(Integer totalExerciseSessions) {
        this.totalExerciseSessions = totalExerciseSessions;
    }

    public Integer getTotalSetsSessions() {
        return totalSetsSessions;
    }

    public void setTotalSetsSessions(Integer totalSetsSessions) {
        this.totalSetsSessions = totalSetsSessions;
    }

    public Double getTotalWeightLifted() {
        return totalWeightLifted;
    }

    public void setTotalWeightLifted(Double totalWeightLifted) {
        this.totalWeightLifted = totalWeightLifted;
    }

    public Integer getConsecutiveWorkoutDays() {
        return consecutiveWorkoutDays;
    }

    public void setConsecutiveWorkoutDays(Integer consecutiveWorkoutDays) {
        this.consecutiveWorkoutDays = consecutiveWorkoutDays;
    }

    public LocalDateTime getLastWorkoutDate() {
        return lastWorkoutDate;
    }

    public void setLastWorkoutDate(LocalDateTime lastWorkoutDate) {
        this.lastWorkoutDate = lastWorkoutDate;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public Integer getLastWorkoutDay() {
        return lastWorkoutDay;
    }

    public void setLastWorkoutDay(Integer lastWorkoutDay) {
        this.lastWorkoutDay = lastWorkoutDay;
    }

    public Integer getLastExerciseSessionId() {
        return lastExerciseSessionId;
    }

    public void setLastExerciseSessionId(Integer lastExerciseSessionId) {
        this.lastExerciseSessionId = lastExerciseSessionId;
    }

    public String getWorkoutSplit() {
        return workoutSplit;
    }

    public void setWorkoutSplit(String workoutSplit) {
        this.workoutSplit = workoutSplit;
    }

    public String getWorkoutLocation() {
        return workoutLocation;
    }

    public void setWorkoutLocation(String workoutLocation) {
        this.workoutLocation = workoutLocation;
    }

    public Double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(Double weightKg) {
        this.weightKg = weightKg;
    }

    public Double getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(Double heightCm) {
        this.heightCm = heightCm;
    }

    // Getters and Setters for new fields
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

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    // Helper methods
    public void incrementTotalWorkouts() {
        this.totalWorkouts++;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public void incrementExerciseSessions(int count) {
        this.totalExerciseSessions += count;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public void incrementSetsSessions(int count) {
        this.totalSetsSessions += count;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public void addWeightLifted(Double weight) {
        this.totalWeightLifted += weight;
        this.lastUpdatedAt = LocalDateTime.now();
    }
}

