package com.pipeline.platform.source.api.request;

import com.pipeline.platform.source.domain.AuthType;
import com.pipeline.platform.source.domain.CodeSourceProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "创建代码源连接请求")
public record CreateCodeSourceRequest(
        @Schema(description = "代码源名称", example = "本地 Gitea")
        @NotBlank @Size(max = 120) String name,
        @Schema(description = "代码平台类型", example = "GITEA")
        @NotNull CodeSourceProvider provider,
        @Schema(description = "代码平台地址", example = "http://localhost:3000")
        @NotBlank @Size(max = 500) String baseUrl,
        @Schema(description = "认证方式。GitLab/Gitea API 验证推荐 ACCESS_TOKEN", example = "ACCESS_TOKEN")
        @NotNull AuthType authType,
        @Schema(description = "用户名，可选", example = "bo")
        @Size(max = 200) String username,
        @Schema(description = "访问令牌。本地 MVP 会明文入库，但响应不会返回完整值。", example = "glpat_xxx")
        String secret
) {
}
