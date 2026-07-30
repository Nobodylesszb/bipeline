package com.pipeline.platform.jenkins.infrastructure.persistence;

import java.time.OffsetDateTime;

import com.pipeline.platform.jenkins.domain.JenkinsJob;
import com.pipeline.platform.jenkins.domain.JenkinsJobSyncStatus;
import com.pipeline.platform.jenkins.domain.JenkinsJobType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "jenkins_jobs")
class JenkinsJobJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pipeline_id", nullable = false)
    private Long pipelineId;

    @Column(name = "jenkins_connection_id", nullable = false)
    private Long jenkinsConnectionId;

    @Column(name = "job_name", nullable = false, length = 500)
    private String jobName;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 32)
    private JenkinsJobType jobType;

    @Column(name = "config_hash", nullable = false, length = 128)
    private String configHash;

    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_sync_status", nullable = false, length = 32)
    private JenkinsJobSyncStatus lastSyncStatus;

    @Column(name = "last_sync_message", length = 1000)
    private String lastSyncMessage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected JenkinsJobJpaEntity() {
    }

    static JenkinsJobJpaEntity from(JenkinsJob jenkinsJob) {
        JenkinsJobJpaEntity entity = new JenkinsJobJpaEntity();
        entity.id = jenkinsJob.id();
        entity.pipelineId = jenkinsJob.pipelineId();
        entity.jenkinsConnectionId = jenkinsJob.jenkinsConnectionId();
        entity.jobName = jenkinsJob.jobName();
        entity.jobType = jenkinsJob.jobType();
        entity.configHash = jenkinsJob.configHash();
        entity.lastSyncedAt = jenkinsJob.lastSyncedAt();
        entity.lastSyncStatus = jenkinsJob.lastSyncStatus();
        entity.lastSyncMessage = jenkinsJob.lastSyncMessage();
        entity.createdAt = jenkinsJob.createdAt();
        entity.updatedAt = jenkinsJob.updatedAt();
        return entity;
    }

    JenkinsJob toDomain() {
        return new JenkinsJob(
                id,
                pipelineId,
                jenkinsConnectionId,
                jobName,
                jobType,
                configHash,
                lastSyncedAt,
                lastSyncStatus,
                lastSyncMessage,
                createdAt,
                updatedAt
        );
    }
}
