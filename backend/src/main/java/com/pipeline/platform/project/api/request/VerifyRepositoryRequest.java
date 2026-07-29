package com.pipeline.platform.project.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "验证代码仓库请求")
public record VerifyRepositoryRequest(
        @NotNull(message = "Code source id is required")
        @Schema(description = "代码源连接 ID", example = "1")
        Long codeSourceId,

        @NotBlank(message = "Repository path is required")
        @Schema(description = "仓库路径，格式为 owner/repo 或 group/repo", example = "bobo_3776/hotel_link_supply")
        String repositoryPath
) {
}
