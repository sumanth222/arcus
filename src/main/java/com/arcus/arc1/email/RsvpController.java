package com.arcus.arc1.email;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rsvps")
public class RsvpController {

    private final com.arcus.arc1.rsvp.RsvpService rsvpService;

    public RsvpController(com.arcus.arc1.rsvp.RsvpService rsvpService) {
        this.rsvpService = rsvpService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> submit(
             @RequestBody RsvpRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(rsvpService.submit(request));
    }
}