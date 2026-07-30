package com.pipeline.platform.pipeline.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.pipeline.platform.pipeline.domain.PipelineStage;
import com.pipeline.platform.pipeline.domain.PipelineStep;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "pipeline_stages")
class PipelineStageJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_id", nullable = false)
    private PipelineJpaEntity pipeline;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "stage", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder asc")
    private List<PipelineStepJpaEntity> steps = new ArrayList<>();

    protected PipelineStageJpaEntity() {
    }

    static PipelineStageJpaEntity from(PipelineStage stage) {
        PipelineStageJpaEntity entity = new PipelineStageJpaEntity();
        entity.id = stage.id();
        entity.name = stage.name();
        entity.displayName = stage.displayName();
        entity.sortOrder = stage.sortOrder();
        entity.createdAt = stage.createdAt();
        entity.updatedAt = stage.updatedAt();
        stage.steps().stream()
                .sorted(Comparator.comparingInt(PipelineStep::sortOrder))
                .map(PipelineStepJpaEntity::from)
                .forEach(entity::addStep);
        return entity;
    }

    PipelineStage toDomain() {
        Long pipelineId = pipeline == null ? null : pipeline.id();
        return new PipelineStage(
                id,
                pipelineId,
                name,
                displayName,
                sortOrder,
                steps.stream()
                        .sorted(Comparator.comparingInt(PipelineStepJpaEntity::sortOrder))
                        .map(PipelineStepJpaEntity::toDomain)
                        .toList(),
                createdAt,
                updatedAt
        );
    }

    void attachTo(PipelineJpaEntity pipeline) {
        this.pipeline = pipeline;
        steps.forEach(step -> step.attachTo(pipeline, this));
    }

    int sortOrder() {
        return sortOrder;
    }

    Long id() {
        return id;
    }

    private void addStep(PipelineStepJpaEntity step) {
        step.attachTo(null, this);
        steps.add(step);
    }
}
