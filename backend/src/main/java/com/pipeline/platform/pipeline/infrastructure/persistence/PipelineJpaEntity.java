package com.pipeline.platform.pipeline.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.pipeline.platform.pipeline.domain.Pipeline;
import com.pipeline.platform.pipeline.domain.PipelineStage;
import com.pipeline.platform.pipeline.domain.PipelineStatus;
import com.pipeline.platform.pipeline.domain.TriggerType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "pipelines")
class PipelineJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PipelineStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 32)
    private TriggerType triggerType;

    @Column(name = "branch_name", nullable = false, length = 200)
    private String branchName;

    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "pipeline", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder asc")
    private List<PipelineStageJpaEntity> stages = new ArrayList<>();

    protected PipelineJpaEntity() {
    }

    static PipelineJpaEntity from(Pipeline pipeline) {
        PipelineJpaEntity entity = new PipelineJpaEntity();
        entity.id = pipeline.id();
        entity.projectId = pipeline.projectId();
        entity.name = pipeline.name();
        entity.description = pipeline.description();
        entity.status = pipeline.status();
        entity.triggerType = pipeline.triggerType();
        entity.branchName = pipeline.branchName();
        entity.version = pipeline.version();
        entity.createdAt = pipeline.createdAt();
        entity.updatedAt = pipeline.updatedAt();
        pipeline.stages().stream()
                .sorted(Comparator.comparingInt(PipelineStage::sortOrder))
                .map(PipelineStageJpaEntity::from)
                .forEach(entity::addStage);
        return entity;
    }

    Pipeline toDomain() {
        return new Pipeline(
                id,
                projectId,
                name,
                description,
                status,
                triggerType,
                branchName,
                version,
                stages.stream()
                        .sorted(Comparator.comparingInt(PipelineStageJpaEntity::sortOrder))
                        .map(PipelineStageJpaEntity::toDomain)
                        .toList(),
                createdAt,
                updatedAt
        );
    }

    private void addStage(PipelineStageJpaEntity stage) {
        stage.attachTo(this);
        stages.add(stage);
    }

    Long id() {
        return id;
    }
}
