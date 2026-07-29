package com.pipeline.platform.project.api.response;

import java.time.OffsetDateTime;

import com.pipeline.platform.project.domain.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "项目响应")
public record ProjectResponse(
        @Schema(description = "项目 ID")
        Long id,

        @Schema(description = "项目名称")
        String name,

        @Schema(description = "项目描述")
        String description,

        @Schema(description = "代码源连接 ID")
        Long codeSourceId,

        @Schema(description = "项目状态")
        ProjectStatus status,

        @Schema(description = "项目绑定的代码仓库，未绑定时为 null")
        ProjectGitRepositoryResponse repository,

        @Schema(description = "创建时间")
        OffsetDateTime createdAt,

        @Schema(description = "更新时间")
        OffsetDateTime updatedAt
) {
}
