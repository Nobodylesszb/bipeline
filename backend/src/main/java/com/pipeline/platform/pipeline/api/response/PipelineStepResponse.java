package com.pipeline.platform.pipeline.api.response;

import com.pipeline.platform.pipeline.domain.StepType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "流水线步骤响应")
public record PipelineStepResponse(
        Long id,
        String name,
        String displayName,
        StepType type,
        int sortOrder,
        Object config,
        boolean enabled
) {
}
