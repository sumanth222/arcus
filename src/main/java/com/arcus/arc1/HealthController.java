package com.arcus.arc1;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@CrossOrigin(origins = "*")
public class HealthController {

    @GetMapping("/ping")
    public String ping() {
        return "Arcus backend is alive! 🚀 " + LocalDateTime.now();
    }
}

