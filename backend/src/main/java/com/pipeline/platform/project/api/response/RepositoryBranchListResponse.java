package com.pipeline.platform.project.api.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "仓库分支列表响应")
public record RepositoryBranchListResponse(
        @Schema(description = "分支列表")
        List<RepositoryBranchResponse> items
) {
}
