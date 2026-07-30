package com.pipeline.platform.pipeline.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "流水线状态变更请求")
public record PipelineStatusChangeRequest(
        @NotNull(message = "Pipeline id is required")
        @Schema(description = "流水线 ID", example = "1")
        Long pipelineId
) {
}
