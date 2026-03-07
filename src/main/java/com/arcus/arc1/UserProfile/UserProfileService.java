package com.arcus.arc1.UserProfile;

import com.arcus.arc1.ExerciseSession.ExerciseSessionEntity;
import com.arcus.arc1.ExerciseSession.ExerciseSessionRepo;
import com.arcus.arc1.SetLog.SetLogEntity;
import com.arcus.arc1.SetLog.SetLogRepo;
import com.arcus.arc1.WorkoutSession.WorkoutSessionEntity;
import com.arcus.arc1.WorkoutSession.WorkoutSessionRepo;
import com.arcus.arc1.dto.UserProfileDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing user profiles.
 * Handles profile creation, retrieval, and statistics updates.
 */
@Service
public class UserProfileService {

    private final UserProfileRepo userProfileRepo;
    private final WorkoutSessionRepo workoutSessionRepo;
    private final ExerciseSessionRepo exerciseSessionRepo;
    private final SetLogRepo setLogRepo;

    public UserProfileService(UserProfileRepo userProfileRepo,
                             WorkoutSessionRepo workoutSessionRepo,
                             ExerciseSessionRepo exerciseSessionRepo,
                             SetLogRepo setLogRepo) {
        this.userProfileRepo = userProfileRepo;
        this.workoutSessionRepo = workoutSessionRepo;
        this.exerciseSessionRepo = exerciseSessionRepo;
        this.setLogRepo = setLogRepo;
    }

    /**
     * Creates a new user profile.
     */
    public UserProfileEntity createUserProfile(Long userId, String name, String email,
                                              String level, String fitnessGoal) {
        if (userProfileRepo.existsByUserId(userId)) {
            throw new IllegalArgumentException("User profile already exists for userId: " + userId);
        }

        UserProfileEntity profile = new UserProfileEntity(userId, name, email, level, fitnessGoal, 0, 0);
        return userProfileRepo.save(profile);
    }

    /**
     * Retrieves user profile by user ID.
     */
    public UserProfileDTO getUserProfile(Long userId) {
        UserProfileEntity profile = userProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found for userId: " + userId));
        return convertToDTO(profile);
    }

    /**
     * Updates user profile information.
     */
    public UserProfileDTO updateUserProfile(Long userId, String name, String email, String bio, String fitnessGoal) {
        UserProfileEntity profile = userProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found for userId: " + userId));

        if (name != null) profile.setName(name);
        if (email != null) profile.setEmail(email);
        if (bio != null) profile.setBio(bio);
        if (fitnessGoal != null) profile.setFitnessGoal(fitnessGoal);

        profile.setLastUpdatedAt(LocalDateTime.now());
        profile = userProfileRepo.save(profile);
        return convertToDTO(profile);
    }

    /**
     * Refreshes user statistics from database.
     */
    public UserProfileDTO refreshUserStats(Long userId) {
        UserProfileEntity profile = userProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found for userId: " + userId));

        List<WorkoutSessionEntity> allWorkouts = workoutSessionRepo.findByUserIdOrderByCreatedAtDesc(userId);
        profile.setTotalWorkouts(allWorkouts.size());

        int totalExerciseSessions = 0;
        int totalSetsSessions = 0;
        Double totalWeightLifted = 0.0;

        for (WorkoutSessionEntity workout : allWorkouts) {
            List<ExerciseSessionEntity> exercises = exerciseSessionRepo.findByWorkoutSessionId(workout.getId());
            totalExerciseSessions += exercises.size();

            for (ExerciseSessionEntity exercise : exercises) {
                Double weight = exercise.getTargetWeight() != null ? exercise.getTargetWeight() : 0.0;
                Integer sets = exercise.getSets() != null ? exercise.getSets() : 0;

                List<SetLogEntity> setLogs = setLogRepo.findByExerciseSessionIdOrderBySetNumberAsc(exercise.getId());
                if (!setLogs.isEmpty()) {
                    for (SetLogEntity setLog : setLogs) {
                        Integer reps = setLog.getReps() != null ? setLog.getReps() : 0;
                        totalWeightLifted += weight * reps;
                    }
                } else {
                    Integer repTarget = exercise.getRepMax() != null ? exercise.getRepMax() : 10;
                    totalWeightLifted += weight * repTarget * sets;
                }
                totalSetsSessions += sets;
            }
        }

        profile.setTotalExerciseSessions(totalExerciseSessions);
        profile.setTotalSetsSessions(totalSetsSessions);
        profile.setTotalWeightLifted(totalWeightLifted);

        if (!allWorkouts.isEmpty()) {
            profile.setLastWorkoutDate(allWorkouts.getFirst().getCreatedAt());
        }

        profile.setLastUpdatedAt(LocalDateTime.now());
        profile = userProfileRepo.save(profile);
        return convertToDTO(profile);
    }

    /**
     * Increments workout count.
     */
    public void incrementWorkoutCount(Long userId) {
        UserProfileEntity profile = userProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found for userId: " + userId));

        profile.incrementTotalWorkouts();
        profile.setLastWorkoutDate(LocalDateTime.now());
        userProfileRepo.save(profile);
    }

    /**
     * Converts UserProfileEntity to UserProfileDTO.
     */
    private UserProfileDTO convertToDTO(UserProfileEntity profile) {
        return new UserProfileDTO(
                profile.getUserId(),
                profile.getName(),
                profile.getEmail(),
                profile.getJoinedAt(),
                profile.getCurrentLevel(),
                profile.getFitnessGoal(),
                profile.getBio(),
                profile.getTotalWorkouts(),
                profile.getTotalExerciseSessions(),
                profile.getTotalSetsSessions(),
                profile.getTotalWeightLifted(),
                profile.getConsecutiveWorkoutDays(),
                profile.getLastWorkoutDate(),
                profile.getLastWorkoutDay(),
                profile.getLastExerciseSessionId()
        );
    }

    /**
     * Gets profile completeness percentage.
     */
    public Integer getProfileCompleteness(Long userId) {
        UserProfileEntity profile = userProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found for userId: " + userId));

        int completeness = 0;
        if (profile.getName() != null && !profile.getName().isEmpty()) completeness += 20;
        if (profile.getEmail() != null && !profile.getEmail().isEmpty()) completeness += 20;
        if (profile.getCurrentLevel() != null) completeness += 20;
        if (profile.getFitnessGoal() != null) completeness += 20;
        if (profile.getBio() != null && !profile.getBio().isEmpty()) completeness += 20;

        return completeness;
    }
}
