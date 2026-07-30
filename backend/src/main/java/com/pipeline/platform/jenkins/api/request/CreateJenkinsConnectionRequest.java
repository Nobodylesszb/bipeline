package com.pipeline.platform.jenkins.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "创建 Jenkins 连接请求")
public record CreateJenkinsConnectionRequest(
        @NotBlank(message = "Jenkins connection name is required")
        @Size(max = 120, message = "Jenkins connection name must be less than 120 characters")
        @Schema(description = "连接名称", example = "本地 Jenkins")
        String name,

        @NotBlank(message = "Jenkins base URL is required")
        @Size(max = 500, message = "Jenkins base URL must be less than 500 characters")
        @Schema(description = "Jenkins 地址", example = "http://localhost:8080")
        String baseUrl,

        @NotBlank(message = "Jenkins username is required")
        @Size(max = 200, message = "Jenkins username must be less than 200 characters")
        @Schema(description = "Jenkins 用户名", example = "admin")
        String username,

        @Schema(description = "Jenkins API Token，第一版本地 MVP 明文保存")
        String apiToken
) {
}
