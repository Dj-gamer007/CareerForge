package com.careerforge.controller;

import com.careerforge.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealthStatus() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("service", "CareerForge Core API");
        healthInfo.put("version", "1.0.0");
        healthInfo.put("phase", "Phase 1 - Core Initialization");

        return ResponseEntity.ok(ApiResponse.success("CareerForge Service is operational", healthInfo));
    }
}
