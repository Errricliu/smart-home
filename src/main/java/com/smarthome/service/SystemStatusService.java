package com.smarthome.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.smarthome.repository.SystemStatusRepository;

@Service
public class SystemStatusService {

    private final SystemStatusRepository systemStatusRepository;

    public SystemStatusService(SystemStatusRepository systemStatusRepository) {
        this.systemStatusRepository = systemStatusRepository;
    }

    public Map<String, Object> getBackendStatus() {
        return Map.of(
                "success", true,
                "message", "Smart Home backend is running",
                "timestamp", systemStatusRepository.getCurrentTime().toString()
        );
    }
}
