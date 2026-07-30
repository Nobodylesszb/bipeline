package com.pipeline.platform.jenkins.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "验证 Jenkins 连接请求")
public record VerifyJenkinsConnectionRequest(
        @NotNull(message = "Jenkins connection id is required")
        @Schema(description = "Jenkins 连接 ID", example = "1")
        Long connectionId
) {
}
