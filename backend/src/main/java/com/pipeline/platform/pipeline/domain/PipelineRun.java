package com.pipeline.platform.pipeline.domain;

import java.time.OffsetDateTime;

import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;

public record PipelineRun(
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

    public static PipelineRun start(
            Pipeline pipeline,
            Long jenkinsConnectionId,
            int runNumber,
            String branch,
            String commitSha,
            String jenkinsJobName,
            OffsetDateTime now
    ) {
        if (pipeline.status() != PipelineStatus.ACTIVE) {
            throw new BusinessException(
                    ErrorCode.PIPELINE_NOT_RUNNABLE,
                    "Only active pipeline can be started"
            );
        }
        return new PipelineRun(
                null,
                pipeline.id(),
                pipeline.projectId(),
                jenkinsConnectionId,
                runNumber,
                PipelineRunStatus.PENDING,
                TriggerType.MANUAL,
                defaultBranch(branch, pipeline.branchName()),
                blankToNull(commitSha),
                jenkinsJobName,
                null,
                null,
                null,
                null,
                now,
                now
        );
    }

    public PipelineRun markQueued(String queueUrl, OffsetDateTime now) {
        return new PipelineRun(
                id,
                pipelineId,
                projectId,
                jenkinsConnectionId,
                runNumber,
                PipelineRunStatus.QUEUED,
                triggerType,
                branch,
                commitSha,
                jenkinsJobName,
                queueUrl,
                null,
                null,
                null,
                createdAt,
                now
        );
    }

    public PipelineRun markRunning(String queueUrl, Integer buildNumber, OffsetDateTime now) {
        return new PipelineRun(
                id,
                pipelineId,
                projectId,
                jenkinsConnectionId,
                runNumber,
                PipelineRunStatus.RUNNING,
                triggerType,
                branch,
                commitSha,
                jenkinsJobName,
                queueUrl,
                buildNumber,
                startedAt == null ? now : startedAt,
                finishedAt,
                createdAt,
                now
        );
    }

    public PipelineRun finish(PipelineRunStatus terminalStatus, OffsetDateTime now) {
        if (terminalStatus != PipelineRunStatus.SUCCESS
                && terminalStatus != PipelineRunStatus.FAILED
                && terminalStatus != PipelineRunStatus.CANCELED) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "Pipeline run terminal status is invalid"
            );
        }
        return new PipelineRun(
                id,
                pipelineId,
                projectId,
                jenkinsConnectionId,
                runNumber,
                terminalStatus,
                triggerType,
                branch,
                commitSha,
                jenkinsJobName,
                jenkinsQueueUrl,
                jenkinsBuildNumber,
                startedAt,
                now,
                createdAt,
                now
        );
    }

    public boolean isTerminal() {
        return status == PipelineRunStatus.SUCCESS
                || status == PipelineRunStatus.FAILED
                || status == PipelineRunStatus.CANCELED;
    }

    private static String defaultBranch(String requestedBranch, String pipelineBranch) {
        if (requestedBranch == null || requestedBranch.isBlank()) {
            return pipelineBranch;
        }
        return requestedBranch.trim();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
