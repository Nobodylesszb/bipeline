package com.pipeline.platform.source.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "测试代码源连通性请求")
public record VerifyCodeSourceRequest(
        @Schema(description = "可选：指定仓库路径。当前先预留，后续用于验证具体仓库是否可访问。", example = "group/order-service")
        @Size(max = 500) String repositoryPath
) {
}
