package com.pipeline.platform.pipeline.domain;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

public record PipelineRunLog(
        Long id,
        Long pipelineRunId,
        String externalLogUrl,
        String logExcerpt,
        long logSizeBytes,
        OffsetDateTime fetchedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    private static final int DEFAULT_MAX_LINES = 200;

    public static PipelineRunLog capture(
            Long pipelineRunId,
            String externalLogUrl,
            String fullLog,
            OffsetDateTime now
    ) {
        String safeLog = fullLog == null ? "" : fullLog;
        return new PipelineRunLog(
                null,
                pipelineRunId,
                externalLogUrl,
                lastLines(safeLog, DEFAULT_MAX_LINES),
                safeLog.getBytes(StandardCharsets.UTF_8).length,
                now,
                now,
                now
        );
    }

    public PipelineRunLog replaceWith(String externalLogUrl, String fullLog, OffsetDateTime now) {
        String safeLog = fullLog == null ? "" : fullLog;
        return new PipelineRunLog(
                id,
                pipelineRunId,
                externalLogUrl,
                lastLines(safeLog, DEFAULT_MAX_LINES),
                safeLog.getBytes(StandardCharsets.UTF_8).length,
                now,
                createdAt,
                now
        );
    }

    private static String lastLines(String content, int maxLines) {
        String[] lines = content.split("\\R", -1);
        if (lines.length <= maxLines) {
            return content;
        }
        StringBuilder excerpt = new StringBuilder();
        int start = lines.length - maxLines;
        for (int index = start; index < lines.length; index++) {
            if (excerpt.length() > 0) {
                excerpt.append(System.lineSeparator());
            }
            excerpt.append(lines[index]);
        }
        return excerpt.toString();
    }
}
