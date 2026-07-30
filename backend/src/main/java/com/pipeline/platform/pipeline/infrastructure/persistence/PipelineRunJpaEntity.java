package com.pipeline.platform.pipeline.infrastructure.persistence;

import java.time.OffsetDateTime;

import com.pipeline.platform.pipeline.domain.PipelineRun;
import com.pipeline.platform.pipeline.domain.PipelineRunStatus;
import com.pipeline.platform.pipeline.domain.TriggerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pipeline_runs")
class PipelineRunJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pipeline_id", nullable = false)
    private Long pipelineId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "jenkins_connection_id", nullable = false)
    private Long jenkinsConnectionId;

    @Column(name = "run_number", nullable = false)
    private int runNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PipelineRunStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 32)
    private TriggerType triggerType;

    @Column(nullable = false, length = 200)
    private String branch;

    @Column(name = "commit_sha", length = 200)
    private String commitSha;

    @Column(name = "jenkins_job_name", nullable = false, length = 500)
    private String jenkinsJobName;

    @Column(name = "jenkins_queue_url", length = 1000)
    private String jenkinsQueueUrl;

    @Column(name = "jenkins_build_number")
    private Integer jenkinsBuildNumber;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected PipelineRunJpaEntity() {
    }

    static PipelineRunJpaEntity from(PipelineRun pipelineRun) {
        PipelineRunJpaEntity entity = new PipelineRunJpaEntity();
        entity.id = pipelineRun.id();
        entity.pipelineId = pipelineRun.pipelineId();
        entity.projectId = pipelineRun.projectId();
        entity.jenkinsConnectionId = pipelineRun.jenkinsConnectionId();
        entity.runNumber = pipelineRun.runNumber();
        entity.status = pipelineRun.status();
        entity.triggerType = pipelineRun.triggerType();
        entity.branch = pipelineRun.branch();
        entity.commitSha = pipelineRun.commitSha();
        entity.jenkinsJobName = pipelineRun.jenkinsJobName();
        entity.jenkinsQueueUrl = pipelineRun.jenkinsQueueUrl();
        entity.jenkinsBuildNumber = pipelineRun.jenkinsBuildNumber();
        entity.startedAt = pipelineRun.startedAt();
        entity.finishedAt = pipelineRun.finishedAt();
        entity.createdAt = pipelineRun.createdAt();
        entity.updatedAt = pipelineRun.updatedAt();
        return entity;
    }

    PipelineRun toDomain() {
        return new PipelineRun(
                id,
                pipelineId,
                projectId,
                jenkinsConnectionId,
                runNumber,
                status,
                triggerType,
                branch,
                commitSha,
                jenkinsJobName,
                jenkinsQueueUrl,
                jenkinsBuildNumber,
                startedAt,
                finishedAt,
                createdAt,
                updatedAt
        );
    }
}
