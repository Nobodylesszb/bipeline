package com.pipeline.platform.pipeline.application.model;

import com.pipeline.platform.pipeline.domain.PipelineStep;
import com.pipeline.platform.pipeline.domain.StepType;

public record PipelineStepView(
        Long id,
        String name,
        String displayName,
        StepType type,
        int sortOrder,
        String configJson,
        boolean enabled
) {

    public static PipelineStepView from(PipelineStep step) {
        return new PipelineStepView(
                step.id(),
                step.name(),
                step.displayName(),
                step.type(),
                step.sortOrder(),
                step.configJson(),
                step.enabled()
        );
    }
}
