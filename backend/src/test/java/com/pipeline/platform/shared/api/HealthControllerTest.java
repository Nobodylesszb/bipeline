package com.pipeline.platform.shared.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.pipeline.platform.shared.time.ClockProvider;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class HealthControllerTest {

    @Test
    void returnsHealthStatus() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-07-29T04:00:00Z"), ZoneOffset.UTC);
        HealthController controller = new HealthController();
        ReflectionTestUtils.setField(controller, "clockProvider", new ClockProvider(fixedClock));

        HealthController.HealthResponse response = controller.health();

        assertThat(response.status()).isEqualTo("UP");
        assertThat(response.checkedAt()).isEqualTo("2026-07-29T04:00Z");
    }
}
