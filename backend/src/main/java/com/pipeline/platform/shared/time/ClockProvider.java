package com.pipeline.platform.shared.time;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;

@Component
public class ClockProvider {

    private final Clock clock;

    public ClockProvider() {
        this(Clock.systemDefaultZone());
    }

    public ClockProvider(Clock clock) {
        this.clock = clock;
    }

    public OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
