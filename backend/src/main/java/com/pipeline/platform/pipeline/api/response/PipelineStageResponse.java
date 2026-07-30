package com.pipeline.platform.pipeline.api.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "流水线阶段响应")
public record PipelineStageResponse(
        Long id,
        String name,
        String displayName,
        int sortOrder,
        List<PipelineStepResponse> steps
) {
}
