package com.pipeline.platform.shared.api;

import java.time.OffsetDateTime;

import com.pipeline.platform.shared.time.ClockProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final ClockProvider clockProvider;

    public HealthController(ClockProvider clockProvider) {
        this.clockProvider = clockProvider;
    }

    @GetMapping
    public HealthResponse health() {
        return new HealthResponse("UP", clockProvider.now());
    }

    public record HealthResponse(String status, OffsetDateTime checkedAt) {
    }
}
