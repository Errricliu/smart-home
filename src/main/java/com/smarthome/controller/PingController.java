package com.smarthome.controller;

import java.util.Map;

import com.smarthome.service.SystemStatusService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PingController {

    private final SystemStatusService systemStatusService;

    public PingController(SystemStatusService systemStatusService) {
        this.systemStatusService = systemStatusService;
    }

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return systemStatusService.getBackendStatus();
    }
}
