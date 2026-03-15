package com.arcus.arc1.UserCredentials;

import com.arcus.arc1.dto.LoginRequest;
import com.arcus.arc1.dto.LoginResponse;
import org.springframework.web.bind.annotation.*;

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
    public java.util.Map<String, Boolean> checkUsername(@RequestParam String username) {
        boolean available = !authService.isUsernameTaken(username);
        return java.util.Map.of("available", available);
    }
}


