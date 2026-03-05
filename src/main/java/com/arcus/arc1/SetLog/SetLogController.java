package com.arcus.arc1.SetLog;

import com.arcus.arc1.dto.SetLogDTO;
import com.arcus.arc1.dto.SetEvaluationDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for logging sets and retrieving set evaluation feedback.
 */
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