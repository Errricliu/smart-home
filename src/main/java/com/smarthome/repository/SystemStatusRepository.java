package com.smarthome.repository;

import java.time.Instant;

import org.springframework.stereotype.Repository;

@Repository
public class SystemStatusRepository {

    public Instant getCurrentTime() {
        return Instant.now();
    }
}
