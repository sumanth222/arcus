package com.arcus.arc1.UserProfile;

import com.arcus.arc1.ExerciseSession.ExerciseSessionEntity;
import com.arcus.arc1.ExerciseSession.ExerciseSessionRepo;
import com.arcus.arc1.SetLog.SetLogEntity;
import com.arcus.arc1.SetLog.SetLogRepo;
import com.arcus.arc1.UserCredentials.UserCredentials;
import com.arcus.arc1.UserCredentials.UserCredentialsRepository;
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
    private final UserCredentialsRepository credentialsRepo;

    public UserProfileService(UserProfileRepo userProfileRepo,
                             WorkoutSessionRepo workoutSessionRepo,
                             ExerciseSessionRepo exerciseSessionRepo,
                             SetLogRepo setLogRepo,
                             UserCredentialsRepository credentialsRepo) {
        this.userProfileRepo = userProfileRepo;
        this.workoutSessionRepo = workoutSessionRepo;
        this.exerciseSessionRepo = exerciseSessionRepo;
        this.setLogRepo = setLogRepo;
        this.credentialsRepo = credentialsRepo;
    }

    /**
     * Creates a new user profile.
     * If userId is null (new user), auto-generates one from the saved entity's id.
     * If credentialsId is provided, links the UserCredentials to this profile.
     */
    public UserProfileDTO createUserProfile(Long userId, String name, String email,
                                              String level, String fitnessGoal,
                                              String workoutSplit, Integer lastWorkoutDay,
                                              Long credentialsId) {
        if (userId != null && userProfileRepo.existsByUserId(userId)) {
            throw new IllegalArgumentException("User profile already exists for userId: " + userId);
        }

        UserProfileEntity profile = new UserProfileEntity(userId, name, email, level, fitnessGoal,
                lastWorkoutDay != null ? lastWorkoutDay : 0, 0);
        profile.setWorkoutSplit(workoutSplit);

        // If userId is null, we need to save first to get the auto-generated id,
        // then use that id as the userId
        if (userId == null) {
            profile.setUserId(0L); // temporary non-null value to pass DB constraint
            profile = userProfileRepo.save(profile);
            profile.setUserId(profile.getId());
            profile = userProfileRepo.save(profile);
        } else {
            profile = userProfileRepo.save(profile);
        }

        // Link the auth credentials to this profile
        if (credentialsId != null) {
            UserCredentials credentials = credentialsRepo.findById(credentialsId).orElse(null);
            if (credentials != null) {
                credentials.setUserId(profile.getUserId());
                credentialsRepo.save(credentials);
            }
        }

        return convertToDTO(profile);
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
    public UserProfileDTO updateUserProfile(Long userId, String name, String email, String bio, String fitnessGoal, Integer lastWorkoutDay) {
        UserProfileEntity profile = userProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found for userId: " + userId));

        if (name != null) profile.setName(name);
        if (email != null) profile.setEmail(email);
        if (bio != null) profile.setBio(bio);
        if (fitnessGoal != null) profile.setFitnessGoal(fitnessGoal);
        if (lastWorkoutDay != null) profile.setLastWorkoutDay(lastWorkoutDay);

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
        UserProfileDTO dto = new UserProfileDTO(
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
        dto.setWorkoutSplit(profile.getWorkoutSplit());
        return dto;
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
