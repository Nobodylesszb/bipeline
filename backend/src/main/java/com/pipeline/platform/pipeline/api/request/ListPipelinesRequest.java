package com.pipeline.platform.pipeline.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "查询流水线列表请求")
public record ListPipelinesRequest(
        @NotNull(message = "Project id is required")
        @Schema(description = "项目 ID", example = "1")
        Long projectId
) {
}
