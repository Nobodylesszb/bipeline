package com.pipeline.platform.pipeline.application.model;

import java.time.OffsetDateTime;

import com.pipeline.platform.pipeline.domain.PipelineRunLog;

public record PipelineRunLogView(
        Long id,
        Long pipelineRunId,
        String externalLogUrl,
        String logExcerpt,
        long logSizeBytes,
        OffsetDateTime fetchedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static PipelineRunLogView from(PipelineRunLog pipelineRunLog) {
        return new PipelineRunLogView(
                pipelineRunLog.id(),
                pipelineRunLog.pipelineRunId(),
                pipelineRunLog.externalLogUrl(),
                pipelineRunLog.logExcerpt(),
                pipelineRunLog.logSizeBytes(),
                pipelineRunLog.fetchedAt(),
                pipelineRunLog.createdAt(),
                pipelineRunLog.updatedAt()
        );
    }
}
