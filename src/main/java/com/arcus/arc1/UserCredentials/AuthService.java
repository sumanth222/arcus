package com.arcus.arc1.UserCredentials;

import com.arcus.arc1.UserProfile.UserProfileEntity;
import com.arcus.arc1.UserProfile.UserProfileRepo;
import com.arcus.arc1.dto.LoginRequest;
import com.arcus.arc1.dto.LoginResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

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

    /**
     * Google Sign-In — userInfo has already been verified by Google on the frontend.
     * We trust email/sub from it directly and look up or create the user.
     */
    public LoginResponse googleSignIn(Map<String, String> userInfo) {
        String googleSub = userInfo.get("sub");
        String email     = userInfo.get("email");
        String name      = userInfo.get("name");

        if (googleSub == null || googleSub.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing sub in userInfo");
        }

        // 1. Try to find existing user by googleSub (fastest)
        UserCredentials credentials = credentialsRepo.findByGoogleSub(googleSub).orElse(null);

        // 2. Fall back to email lookup (user may have registered before Google Sign-In)
        if (credentials == null && email != null) {
            credentials = credentialsRepo.findByEmail(email).orElse(null);
            if (credentials != null) {
                credentials.setGoogleSub(googleSub);
                credentials = credentialsRepo.save(credentials);
            }
        }

        // 3. Brand new user — create credentials record (no password)
        if (credentials == null) {
            String baseUsername = email != null ? email.split("@")[0] : "user";
            String username = generateUniqueUsername(baseUsername);
            credentials = new UserCredentials(username, email, googleSub, true);

            // Auto-link existing profile by email if found
            if (email != null) {
                UserProfileEntity profileByEmail = userProfileRepo.findByEmail(email).orElse(null);
                if (profileByEmail != null) {
                    credentials.setUserId(profileByEmail.getUserId());
                }
            }

            credentials = credentialsRepo.save(credentials);

            // If we found and linked a profile, return as existing user
            if (credentials.getUserId() != null) {
                UserProfileEntity profile = userProfileRepo.findByUserId(credentials.getUserId()).orElse(null);
                String displayName = (profile != null && profile.getName() != null) ? profile.getName()
                        : (name != null ? name : credentials.getUsername());
                return new LoginResponse(credentials.getUserId(), credentials.getId(), displayName, false, credentials.getUsername());
            }

            return new LoginResponse(null, credentials.getId(), name != null ? name : username, true, username);
        }

        // 4. Existing user — look up profile
        Long userId = credentials.getUserId();

        // If userId is null, check if a profile exists with this email and auto-link it
        if (userId == null && email != null) {
            UserProfileEntity profileByEmail = userProfileRepo.findByEmail(email).orElse(null);
            if (profileByEmail != null) {
                userId = profileByEmail.getUserId();
                credentials.setUserId(userId);
                credentialsRepo.save(credentials);
            }
        }

        if (userId == null) {
            return new LoginResponse(null, credentials.getId(),
                    name != null ? name : credentials.getUsername(), true, credentials.getUsername());
        }

        UserProfileEntity profile = userProfileRepo.findByUserId(userId).orElse(null);
        String displayName = (profile != null && profile.getName() != null) ? profile.getName()
                : (name != null ? name : credentials.getUsername());

        return new LoginResponse(userId, credentials.getId(), displayName, false, credentials.getUsername());
    }

    /**
     * Generates a unique username by appending a number suffix if taken.
     */
    private String generateUniqueUsername(String base) {
        // Sanitize: keep only alphanumeric and underscores
        String clean = base.replaceAll("[^a-zA-Z0-9_]", "");
        if (clean.isBlank()) clean = "user";

        if (!credentialsRepo.existsByUsername(clean)) return clean;

        int i = 1;
        while (credentialsRepo.existsByUsername(clean + i)) i++;
        return clean + i;
    }
}


