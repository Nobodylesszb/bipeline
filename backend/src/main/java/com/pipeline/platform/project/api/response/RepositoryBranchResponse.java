package com.pipeline.platform.project.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "仓库分支响应")
public record RepositoryBranchResponse(
        @Schema(description = "分支名称")
        String name,

        @Schema(description = "分支当前提交 ID")
        String commitId
) {
}
