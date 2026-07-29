package com.pipeline.platform.project.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "项目绑定代码仓库请求")
public record BindProjectRepositoryRequest(
        @NotNull(message = "Project id is required")
        @Schema(description = "项目 ID", example = "1")
        Long projectId,

        @NotNull(message = "Code source id is required")
        @Schema(description = "代码源连接 ID，必须与项目创建时选择的代码源一致", example = "1")
        Long codeSourceId,

        @NotBlank(message = "Repository path is required")
        @Schema(description = "仓库路径", example = "bobo_3776/hotel_link_supply")
        String repositoryPath,

        @NotBlank(message = "Default branch is required")
        @Schema(description = "默认构建分支", example = "master")
        String defaultBranch,

        @Schema(description = "构建上下文目录。单仓库单服务一般填 .", example = ".")
        String contextDirectory
) {
}
