package com.arcus.arc1.UserProfile;

import com.arcus.arc1.dto.CreateProfileRequest;
import com.arcus.arc1.dto.UpdateProfileRequest;
import com.arcus.arc1.dto.UserProfileDTO;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for user profile endpoints.
 *
 * Endpoints:
 * - POST /user/profile/create - Create new user profile
 * - GET /user/profile/{userId} - Get user profile
 * - PUT /user/profile/{userId} - Update user profile
 * - GET /user/profile/{userId}/refresh - Refresh statistics
 * - GET /user/profile/{userId}/completeness - Get profile completeness
 */
@CrossOrigin
@RestController
@RequestMapping("/user/profile")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    /**
     * Creates a new user profile.
     *
     * @param request JSON body with name, email, currentLevel, fitnessGoal, workoutSplit, lastWorkoutDay
     * @return Created UserProfileDTO
     */
    @PostMapping("/create")
    public UserProfileDTO createProfile(@RequestBody CreateProfileRequest request) {

        return userProfileService.createUserProfile(
                request.getUserId(),
                request.getUsername(),
                request.getName(),
                request.getEmail(),
                request.getCurrentLevel(),
                request.getFitnessGoal(),
                request.getWorkoutSplit(),
                request.getLastWorkoutDay(),
                request.getCredentialsId(),
                request.getWeightKg(),
                request.getHeightCm()
        );
    }

    /**
     * Retrieves user profile by user ID.
     *
     * @param userId User ID
     * @return UserProfileDTO with all profile information
     */
    @GetMapping("/{userId}")
    public UserProfileDTO getProfile(@PathVariable Long userId) {
        return userProfileService.getUserProfile(userId);
    }

    /**
     * Updates user profile information.
     *
     * @param userId User ID
     * @param name Updated name (optional)
     * @param email Updated email (optional)
     * @param bio Updated bio (optional)
     * @param fitnessGoal Updated fitness goal (optional)
     * @param lastWorkoutDay Updated last workout day (optional)
     * @return Updated UserProfileDTO
     */
    @PutMapping("/{userId}")
    public UserProfileDTO updateProfile(
            @PathVariable Long userId,
            @RequestBody UpdateProfileRequest request) {

        return userProfileService.updateUserProfile(
                userId,
                request.getName(),
                request.getEmail(),
                request.getBio(),
                request.getFitnessGoal(),
                request.getCurrentLevel(),
                request.getWorkoutSplit(),
                request.getHeightCm(),
                request.getWeightKg(),
                request.getLastWorkoutDay()
        );
    }

    /**
     * Refreshes and recalculates user statistics from database.
     *
     * @param userId User ID
     * @return Updated UserProfileDTO with fresh statistics
     */
    @GetMapping("/{userId}/refresh")
    public UserProfileDTO refreshStats(@PathVariable Long userId) {
        return userProfileService.refreshUserStats(userId);
    }

    /**
     * Gets profile completeness percentage.
     * Shows how much of the profile the user has filled out.
     *
     * @param userId User ID
     * @return Completeness percentage (0-100)
     */
    @GetMapping("/{userId}/completeness")
    public Integer getCompleteness(@PathVariable Long userId) {
        return userProfileService.getProfileCompleteness(userId);
    }

    /**
     * Marks the current workout day as completed and increments lastWorkoutDay.
     * @param userId User ID
     * @return Updated UserProfileDTO
     */
    @PostMapping("/{userId}/{dayNum}/complete-day")
    public UserProfileDTO completeWorkoutDay(@PathVariable Long userId, @PathVariable Long dayNum) {
        return userProfileService.completeWorkoutDay(userId, dayNum);
    }
}
