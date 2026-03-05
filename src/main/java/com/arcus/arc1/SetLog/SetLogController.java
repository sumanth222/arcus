package com.arcus.arc1.SetLog;

import com.arcus.arc1.dto.SetLogDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("logs")
public class SetLogController {

    private final SetLogService setLogService;

    public SetLogController(SetLogService setLogService) {
        this.setLogService = setLogService;
    }

    @PostMapping("/log-set")
    public String logSet(@RequestBody SetLogDTO request) {
        setLogService.saveLog(request);
        return "Set logged";
    }
}