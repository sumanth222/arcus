package com.arcus.arc1.UserCredentials;

import com.arcus.arc1.UserProfile.UserProfileEntity;
import com.arcus.arc1.UserProfile.UserProfileRepo;
import com.arcus.arc1.dto.LoginRequest;
import com.arcus.arc1.dto.LoginResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserCredentialsRepository credentialsRepo;
    private final UserProfileRepo userProfileRepo;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserCredentialsRepository credentialsRepo,
                       UserProfileRepo userProfileRepo,
                       BCryptPasswordEncoder passwordEncoder) {
        this.credentialsRepo = credentialsRepo;
        this.userProfileRepo = userProfileRepo;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user with username and password.
     * Profile is not created yet — userId remains null until onboarding.
     */
    public LoginResponse register(LoginRequest request) {
        if (credentialsRepo.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        UserCredentials credentials = new UserCredentials(request.getUsername(), hashedPassword);
        credentialsRepo.save(credentials);

        return new LoginResponse(null, request.getUsername(), true);
    }

    /**
     * Logs in a user by verifying username and password.
     * Returns profile info if onboarding is complete, otherwise flags as new user.
     */
    public LoginResponse login(LoginRequest request) {
        UserCredentials credentials = credentialsRepo.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), credentials.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        Long userId = credentials.getUserId();

        if (userId == null) {
            // Onboarding not done yet
            return new LoginResponse(null, request.getUsername(), true);
        }

        // Look up the linked profile for the user's name
        UserProfileEntity profile = userProfileRepo.findByUserId(userId).orElse(null);
        String name = (profile != null) ? profile.getName() : request.getUsername();

        return new LoginResponse(userId, name, false);
    }

    /**
     * Checks if a username is already taken.
     */
    public boolean isUsernameTaken(String username) {
        return credentialsRepo.existsByUsername(username);
    }
}


