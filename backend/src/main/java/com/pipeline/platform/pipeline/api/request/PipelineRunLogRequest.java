package com.pipeline.platform.pipeline.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "流水线运行日志请求")
public record PipelineRunLogRequest(
        @NotNull
        @Schema(description = "流水线运行 ID", example = "6")
        Long pipelineRunId
) {
}
