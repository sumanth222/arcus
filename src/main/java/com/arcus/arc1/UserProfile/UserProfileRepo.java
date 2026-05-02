package com.arcus.arc1.UserProfile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User Profile data access.
 */
@Repository
public interface UserProfileRepo extends JpaRepository<UserProfileEntity, Long> {

    /**
     * Find user profile by user ID.
     */
    Optional<UserProfileEntity> findByUserId(Long userId);

    /**
     * Check if a user profile exists by user ID.
     */
    boolean existsByUserId(Long userId);

    Optional<UserProfileEntity> findByEmail(String email);
}

