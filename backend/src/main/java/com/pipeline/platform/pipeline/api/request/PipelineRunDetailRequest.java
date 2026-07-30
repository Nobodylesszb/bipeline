package com.pipeline.platform.pipeline.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "流水线运行详情请求")
public record PipelineRunDetailRequest(
        @NotNull(message = "Pipeline run id is required")
        @Schema(description = "流水线运行 ID", example = "1")
        Long pipelineRunId
) {
}
