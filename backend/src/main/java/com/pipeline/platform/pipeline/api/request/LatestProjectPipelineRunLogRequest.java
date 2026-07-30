package com.pipeline.platform.pipeline.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "项目最近一次流水线运行日志请求")
public record LatestProjectPipelineRunLogRequest(
        @NotNull
        @Schema(description = "项目 ID", example = "1")
        Long projectId,

        @Schema(description = "流水线 ID。为空时查询项目下最近一次构建", example = "1")
        Long pipelineId
) {
}
