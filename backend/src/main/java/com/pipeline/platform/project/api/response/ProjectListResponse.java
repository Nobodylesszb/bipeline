package com.pipeline.platform.project.api.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "项目列表响应")
public record ProjectListResponse(
        @Schema(description = "项目列表")
        List<ProjectResponse> items
) {
}
