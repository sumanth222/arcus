package com.arcus.arc1.SetLog;

import com.arcus.arc1.dto.SetLogDTO;
import com.arcus.arc1.dto.SetEvaluationDTO;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for logging sets and retrieving set evaluation feedback.
 */
@CrossOrigin
@RestController
@RequestMapping("logs")
public class SetLogController {

    private final SetLogService setLogService;

    public SetLogController(SetLogService setLogService) {
        this.setLogService = setLogService;
    }

    /**
     * Logs a set and returns evaluation feedback including fatigue detection and rest recommendations.
     *
     * @param request SetLogDTO containing exerciseSessionId, setNumber, weight, and reps
     * @return SetEvaluationDTO with fatigueDetected, suggestedRestSeconds, and message
     */
    @PostMapping("/log-set")
    public SetEvaluationDTO logSet(@RequestBody SetLogDTO request) {
        return setLogService.saveLogAndEvaluate(request);
    }
}