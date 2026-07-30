package com.pipeline.platform.pipeline.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class PipelineRunLogTest {

    @Test
    void keepsOnlyLastTwoHundredLines() {
        String fullLog = IntStream.rangeClosed(1, 250)
                .mapToObj(index -> "line-" + index)
                .collect(Collectors.joining("\n"));

        PipelineRunLog log = PipelineRunLog.capture(
                6L,
                "http://jenkins/job/main/1/consoleText",
                fullLog,
                OffsetDateTime.parse("2026-07-30T04:00:00Z")
        );

        assertThat(log.logExcerpt()).startsWith("line-51");
        assertThat(log.logExcerpt()).contains("line-250");
        assertThat(log.logExcerpt()).doesNotContain("line-50\n");
        assertThat(log.logSizeBytes()).isGreaterThan(log.logExcerpt().length());
    }
}
