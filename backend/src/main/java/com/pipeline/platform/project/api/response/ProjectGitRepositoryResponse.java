package com.pipeline.platform.project.api.response;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "项目绑定的代码仓库响应")
public record ProjectGitRepositoryResponse(
        @Schema(description = "绑定记录 ID")
        Long id,

        @Schema(description = "项目 ID")
        Long projectId,

        @Schema(description = "仓库路径")
        String remotePath,

        @Schema(description = "仓库 HTTPS 克隆地址")
        String remoteUrl,

        @Schema(description = "默认分支")
        String defaultBranch,

        @Schema(description = "构建上下文目录")
        String contextDirectory,

        @Schema(description = "默认分支当前解析到的提交 ID")
        String lastResolvedRevision,

        @Schema(description = "最近一次拉取仓库信息的时间")
        OffsetDateTime lastFetchedAt,

        @Schema(description = "创建时间")
        OffsetDateTime createdAt,

        @Schema(description = "更新时间")
        OffsetDateTime updatedAt
) {
}
