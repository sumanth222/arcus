package com.arcus.arc1.WorkoutSession;

import com.arcus.arc1.dto.WorkoutResponseDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workout")
public class WorkoutController {

    private final WorkoutGenerationService workoutGenerationService;

    public WorkoutController(WorkoutGenerationService workoutGenerationService) {
        this.workoutGenerationService = workoutGenerationService;
    }

    @PostMapping("/generateWorkout")
    public WorkoutResponseDTO generateWorkout(@RequestParam Long userId,
                                              @RequestParam String level){
        return workoutGenerationService.generateWorkout(userId, level);
    }
 }