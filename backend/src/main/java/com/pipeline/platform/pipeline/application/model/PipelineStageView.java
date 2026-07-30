package com.pipeline.platform.pipeline.application.model;

import java.util.List;

import com.pipeline.platform.pipeline.domain.PipelineStage;

public record PipelineStageView(
        Long id,
        String name,
        String displayName,
        int sortOrder,
        List<PipelineStepView> steps
) {

    public static PipelineStageView from(PipelineStage stage) {
        return new PipelineStageView(
                stage.id(),
                stage.name(),
                stage.displayName(),
                stage.sortOrder(),
                stage.steps().stream()
                        .map(PipelineStepView::from)
                        .toList()
        );
    }
}
