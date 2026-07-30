package com.pipeline.platform.pipeline.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "启动流水线运行请求")
public record StartPipelineRunRequest(
        @NotNull(message = "Pipeline id is required")
        @Schema(description = "流水线 ID", example = "1")
        Long pipelineId,

        @NotNull(message = "Jenkins connection id is required")
        @Schema(description = "Jenkins 连接 ID", example = "2")
        Long jenkinsConnectionId,

        @Schema(description = "运行分支。为空时使用流水线配置分支", example = "master")
        String branch,

        @Schema(description = "提交 SHA。第一版可为空", example = "abc123")
        String commitSha
) {
}
