package com.arcus.arc1.SetLog;

import com.arcus.arc1.dto.SetLogDTO;
import org.springframework.stereotype.Service;

@Service
public class SetLogService {

    private final SetLogRepo setLogRepo;

    public SetLogService(SetLogRepo setLogRepo) {
        this.setLogRepo = setLogRepo;
    }

    public void saveLog(SetLogDTO request) {

        SetLogEntity setLog = new SetLogEntity();
        setLog.setExerciseSessionId(request.getExerciseSessionId());
        setLog.setSetNumber(request.getSetNumber());
        setLog.setWeight(request.getWeight());
        setLog.setReps(request.getReps());

        setLogRepo.save(setLog);
    }
}