package com.arcus.arc1.WorkoutSession;

import com.arcus.arc1.dto.NextWorkoutInfoDTO;
import com.arcus.arc1.dto.WorkoutResponseDTO;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/workout")
public class WorkoutController {

    private final WorkoutGenerationService workoutGenerationService;

    public WorkoutController(WorkoutGenerationService workoutGenerationService) {
        this.workoutGenerationService = workoutGenerationService;
    }

    @GetMapping("/generateWorkout")
    public WorkoutResponseDTO generateWorkout(@RequestParam Long userId,
                                              @RequestParam String level){
        return workoutGenerationService.generateWorkout(userId, level);
    }

    @GetMapping("/nextWorkoutName")
    public NextWorkoutInfoDTO getNextWorkoutName(@RequestParam Long userId,
                                                 @RequestParam String level) {
        return workoutGenerationService.getNextWorkoutInfo(userId, level);
    }

    @PostMapping("/completeWorkout")
    public void finishWorkout(@RequestParam Long userId, @RequestParam double totalWeight) {
        workoutGenerationService.finishWorkout(userId, totalWeight);
    }
 }