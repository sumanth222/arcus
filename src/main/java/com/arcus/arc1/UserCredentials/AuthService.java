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
     * Registers a new user with username, email, and password.
     * Email is required and must be a valid format.
     * Profile is not created yet — userId remains null until onboarding.
     */
    public LoginResponse register(LoginRequest request) {
        // Email is optional during registration — validate format only if provided
        String email = request.getEmail();
        if (email != null && !email.isBlank()) {
            if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email format");
            }
        }

        if (credentialsRepo.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        UserCredentials credentials = (email != null && !email.isBlank())
                ? new UserCredentials(request.getUsername(), hashedPassword, email)
                : new UserCredentials(request.getUsername(), hashedPassword);
        credentials = credentialsRepo.save(credentials);

        return new LoginResponse(null, credentials.getId(), request.getUsername(), true);
    }

    /**
     * Logs in a user by verifying password.
     * Accepts either username or email to identify the account.
     * Returns profile info if onboarding is complete, otherwise flags as new user.
     */
    public LoginResponse login(LoginRequest request) {
        // Resolve credentials by username or email — whichever is provided
        UserCredentials credentials = null;

        String username = request.getUsername();
        String email = request.getEmail();

        if (username != null && !username.isBlank()) {
            credentials = credentialsRepo.findByUsername(username).orElse(null);
        }
        if (credentials == null && email != null && !email.isBlank()) {
            credentials = credentialsRepo.findByEmail(email).orElse(null);
        }

        if (credentials == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username/email or password");
        }

        if (!passwordEncoder.matches(request.getPassword(), credentials.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username/email or password");
        }

        Long userId = credentials.getUserId();

        if (userId == null) {
            // Onboarding not done yet
            return new LoginResponse(null, credentials.getId(), credentials.getUsername(), true);
        }

        // Look up the linked profile for the user's display name
        UserProfileEntity profile = userProfileRepo.findByUserId(userId).orElse(null);
        String name = (profile != null) ? profile.getName() : credentials.getUsername();

        return new LoginResponse(userId, credentials.getId(), name, false);
    }

    /**
     * Checks if a username is already taken.
     */
    public boolean isUsernameTaken(String username) {
        return credentialsRepo.existsByUsername(username);
    }
}


