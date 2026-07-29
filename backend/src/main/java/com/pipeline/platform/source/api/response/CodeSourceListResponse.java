package com.pipeline.platform.source.api.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "代码源连接列表响应")
public record CodeSourceListResponse(
        @Schema(description = "代码源连接列表")
        List<CodeSourceResponse> items
) {
}
