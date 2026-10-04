package com.marketflow.controller;

import com.marketflow.dto.DashboardMetricsResponse;
import com.marketflow.service.ExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/metrics")
@Tag(name = "Metrics", description = "Operational health and dashboard analytics")
public class MetricsController {

    private final ExecutionService executionService;

    public MetricsController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @GetMapping
    @Operation(summary = "Get dashboard metrics", description = "Aggregated execution statistics, success rates, and workflow totals")
    public ResponseEntity<DashboardMetricsResponse> getMetrics() {
        return ResponseEntity.ok(executionService.getMetrics());
    }
}
