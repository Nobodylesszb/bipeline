package com.pipeline.platform.project.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "仓库验证响应")
public record RepositoryVerificationResponse(
        @Schema(description = "仓库路径")
        String repositoryPath,

        @Schema(description = "仓库名称")
        String name,

        @Schema(description = "仓库完整名称")
        String fullName,

        @Schema(description = "默认分支")
        String defaultBranch,

        @Schema(description = "HTTPS 克隆地址")
        String cloneUrl,

        @Schema(description = "SSH 克隆地址")
        String sshUrl,

        @Schema(description = "仓库是否可访问")
        boolean accessible
) {
}
