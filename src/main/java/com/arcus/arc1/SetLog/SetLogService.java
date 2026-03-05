package com.arcus.arc1.SetLog;

import com.arcus.arc1.dto.SetLogDTO;
import com.arcus.arc1.dto.SetEvaluationDTO;
import org.springframework.stereotype.Service;

/**
 * Service for managing set logs and coordinating with evaluation logic.
 */
@Service
public class SetLogService {

    private final SetLogRepo setLogRepo;
    private final SetEvaluationService setEvaluationService;

    public SetLogService(SetLogRepo setLogRepo, SetEvaluationService setEvaluationService) {
        this.setLogRepo = setLogRepo;
        this.setEvaluationService = setEvaluationService;
    }

    /**
     * Saves a set log and returns an evaluation of the set performance.
     *
     * @param request The SetLogDTO containing set information
     * @return SetEvaluationDTO with fatigue detection and rest recommendations
     */
    public SetEvaluationDTO saveLogAndEvaluate(SetLogDTO request) {
        SetLogEntity setLog = new SetLogEntity();
        setLog.setExerciseSessionId(request.getExerciseSessionId());
        setLog.setSetNumber(request.getSetNumber());
        setLog.setWeight(request.getWeight());
        setLog.setReps(request.getReps());

        setLogRepo.save(setLog);

        // Evaluate the set and return feedback
        return setEvaluationService.evaluateSet(request.getExerciseSessionId(), request.getSetNumber());
    }

    /**
     * Legacy method for backwards compatibility - just saves without evaluation.
     *
     * @param request The SetLogDTO containing set information
     */
    public void saveLog(SetLogDTO request) {

        SetLogEntity setLog = new SetLogEntity();
        setLog.setExerciseSessionId(request.getExerciseSessionId());
        setLog.setSetNumber(request.getSetNumber());
        setLog.setWeight(request.getWeight());
        setLog.setReps(request.getReps());

        setLogRepo.save(setLog);
    }
}