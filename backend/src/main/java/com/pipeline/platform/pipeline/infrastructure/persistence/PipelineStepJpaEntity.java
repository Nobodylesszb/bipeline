package com.pipeline.platform.pipeline.infrastructure.persistence;

import java.time.OffsetDateTime;

import com.pipeline.platform.pipeline.domain.PipelineStep;
import com.pipeline.platform.pipeline.domain.StepType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "pipeline_steps")
class PipelineStepJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_id", nullable = false)
    private PipelineJpaEntity pipeline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    private PipelineStageJpaEntity stage;

    @Column(name = "step_key", nullable = false, length = 120)
    private String stepKey;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private StepType type;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "config_json", nullable = false)
    private String configJson;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected PipelineStepJpaEntity() {
    }

    static PipelineStepJpaEntity from(PipelineStep step) {
        PipelineStepJpaEntity entity = new PipelineStepJpaEntity();
        entity.id = step.id();
        entity.stepKey = step.stepKey();
        entity.name = step.name();
        entity.displayName = step.displayName();
        entity.type = step.type();
        entity.sortOrder = step.sortOrder();
        entity.configJson = step.configJson();
        entity.enabled = step.enabled();
        entity.createdAt = step.createdAt();
        entity.updatedAt = step.updatedAt();
        return entity;
    }

    PipelineStep toDomain() {
        return new PipelineStep(
                id,
                pipeline == null ? null : pipeline.id(),
                stage == null ? null : stage.id(),
                stepKey,
                name,
                displayName,
                type,
                sortOrder,
                configJson,
                enabled,
                createdAt,
                updatedAt
        );
    }

    void attachTo(PipelineJpaEntity pipeline, PipelineStageJpaEntity stage) {
        if (pipeline != null) {
            this.pipeline = pipeline;
        }
        this.stage = stage;
    }

    int sortOrder() {
        return sortOrder;
    }
}
