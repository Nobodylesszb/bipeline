package com.pipeline.platform.jenkins.domain;

import java.time.OffsetDateTime;

public record JenkinsJob(
        Long id,
        Long pipelineId,
        Long jenkinsConnectionId,
        String jobName,
        JenkinsJobType jobType,
        String configHash,
        OffsetDateTime lastSyncedAt,
        JenkinsJobSyncStatus lastSyncStatus,
        String lastSyncMessage,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static JenkinsJob create(
            Long pipelineId,
            Long jenkinsConnectionId,
            String jobName,
            JenkinsJobType jobType,
            String configHash,
            OffsetDateTime now
    ) {
        return new JenkinsJob(
                null,
                pipelineId,
                jenkinsConnectionId,
                jobName,
                jobType,
                configHash,
                null,
                JenkinsJobSyncStatus.PENDING,
                null,
                now,
                now
        );
    }

    public JenkinsJob withDefinition(String jobName, JenkinsJobType jobType, String configHash, OffsetDateTime now) {
        return new JenkinsJob(
                id,
                pipelineId,
                jenkinsConnectionId,
                jobName,
                jobType,
                configHash,
                lastSyncedAt,
                JenkinsJobSyncStatus.PENDING,
                null,
                createdAt,
                now
        );
    }

    public JenkinsJob markSynced(String message, OffsetDateTime now) {
        return new JenkinsJob(
                id,
                pipelineId,
                jenkinsConnectionId,
                jobName,
                jobType,
                configHash,
                now,
                JenkinsJobSyncStatus.SYNCED,
                message,
                createdAt,
                now
        );
    }
}
