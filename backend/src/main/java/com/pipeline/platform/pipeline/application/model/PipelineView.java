package com.pipeline.platform.pipeline.application.model;

import java.time.OffsetDateTime;
import java.util.List;

import com.pipeline.platform.pipeline.domain.Pipeline;
import com.pipeline.platform.pipeline.domain.PipelineStatus;
import com.pipeline.platform.pipeline.domain.TriggerType;

public record PipelineView(
        Long id,
        Long projectId,
        String name,
        String description,
        PipelineStatus status,
        TriggerType triggerType,
        String branchName,
        int version,
        List<PipelineStageView> stages,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static PipelineView from(Pipeline pipeline) {
        return new PipelineView(
                pipeline.id(),
                pipeline.projectId(),
                pipeline.name(),
                pipeline.description(),
                pipeline.status(),
                pipeline.triggerType(),
                pipeline.branchName(),
                pipeline.version(),
                pipeline.stages().stream()
                        .map(PipelineStageView::from)
                        .toList(),
                pipeline.createdAt(),
                pipeline.updatedAt()
        );
    }
}
