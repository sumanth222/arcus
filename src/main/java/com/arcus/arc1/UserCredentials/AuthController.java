package com.arcus.arc1.UserCredentials;

import com.arcus.arc1.dto.LoginRequest;
import com.arcus.arc1.dto.LoginResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public LoginResponse register(@RequestBody LoginRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/check-username")
    public Map<String, Boolean> checkUsername(@RequestParam String username) {
        boolean available = !authService.isUsernameTaken(username);
        return Map.of("available", available);
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/google")
    public LoginResponse googleSignIn(@RequestBody Map<String, Object> body) {
        Map<String, String> userInfo = (Map<String, String>) body.get("userInfo");
        if (userInfo == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "userInfo is required");
        }
        return authService.googleSignIn(userInfo);
    }
}


