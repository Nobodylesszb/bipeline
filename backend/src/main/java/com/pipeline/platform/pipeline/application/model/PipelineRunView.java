package com.pipeline.platform.pipeline.application.model;

import java.time.OffsetDateTime;

import com.pipeline.platform.pipeline.domain.PipelineRun;
import com.pipeline.platform.pipeline.domain.PipelineRunStatus;
import com.pipeline.platform.pipeline.domain.TriggerType;

public record PipelineRunView(
        Long id,
        Long pipelineId,
        Long projectId,
        Long jenkinsConnectionId,
        int runNumber,
        PipelineRunStatus status,
        TriggerType triggerType,
        String branch,
        String commitSha,
        String jenkinsJobName,
        String jenkinsQueueUrl,
        Integer jenkinsBuildNumber,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static PipelineRunView from(PipelineRun pipelineRun) {
        return new PipelineRunView(
                pipelineRun.id(),
                pipelineRun.pipelineId(),
                pipelineRun.projectId(),
                pipelineRun.jenkinsConnectionId(),
                pipelineRun.runNumber(),
                pipelineRun.status(),
                pipelineRun.triggerType(),
                pipelineRun.branch(),
                pipelineRun.commitSha(),
                pipelineRun.jenkinsJobName(),
                pipelineRun.jenkinsQueueUrl(),
                pipelineRun.jenkinsBuildNumber(),
                pipelineRun.startedAt(),
                pipelineRun.finishedAt(),
                pipelineRun.createdAt(),
                pipelineRun.updatedAt()
        );
    }
}
