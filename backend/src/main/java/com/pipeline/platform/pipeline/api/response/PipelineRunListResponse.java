package com.pipeline.platform.pipeline.api.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "流水线运行列表响应")
public record PipelineRunListResponse(
        List<PipelineRunResponse> items
) {
}
