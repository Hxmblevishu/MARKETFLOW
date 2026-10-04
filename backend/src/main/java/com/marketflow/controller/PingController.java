package com.marketflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/ping")
@Tag(name = "System", description = "System ping and keep-alive endpoints")
public class PingController {

    private final long startTime = System.currentTimeMillis();

    @GetMapping
    @Operation(summary = "Render keep-alive ping", description = "Lightweight ping endpoint to prevent cloud instances from idling")
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("message", "Render keep-alive ping acknowledged");
        response.put("timestamp", Instant.now().toString());
        response.put("uptimeMs", System.currentTimeMillis() - startTime);
        response.put("jvmUptimeMs", ManagementFactory.getRuntimeMXBean().getUptime());
        return ResponseEntity.ok(response);
    }
}
