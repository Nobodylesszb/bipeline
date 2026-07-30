package com.pipeline.platform.pipeline.infrastructure.persistence;

import java.time.OffsetDateTime;

import com.pipeline.platform.pipeline.domain.PipelineRunLog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pipeline_run_logs")
class PipelineRunLogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pipeline_run_id", nullable = false)
    private Long pipelineRunId;

    @Column(name = "external_log_url", length = 1000)
    private String externalLogUrl;

    @Column(name = "log_excerpt", nullable = false)
    private String logExcerpt;

    @Column(name = "log_size_bytes", nullable = false)
    private long logSizeBytes;

    @Column(name = "fetched_at", nullable = false)
    private OffsetDateTime fetchedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected PipelineRunLogJpaEntity() {
    }

    static PipelineRunLogJpaEntity from(PipelineRunLog pipelineRunLog) {
        PipelineRunLogJpaEntity entity = new PipelineRunLogJpaEntity();
        entity.id = pipelineRunLog.id();
        entity.pipelineRunId = pipelineRunLog.pipelineRunId();
        entity.externalLogUrl = pipelineRunLog.externalLogUrl();
        entity.logExcerpt = pipelineRunLog.logExcerpt();
        entity.logSizeBytes = pipelineRunLog.logSizeBytes();
        entity.fetchedAt = pipelineRunLog.fetchedAt();
        entity.createdAt = pipelineRunLog.createdAt();
        entity.updatedAt = pipelineRunLog.updatedAt();
        return entity;
    }

    PipelineRunLog toDomain() {
        return new PipelineRunLog(
                id,
                pipelineRunId,
                externalLogUrl,
                logExcerpt,
                logSizeBytes,
                fetchedAt,
                createdAt,
                updatedAt
        );
    }
}
