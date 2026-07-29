package com.pipeline.platform.project.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "创建项目请求")
public record CreateProjectRequest(
        @NotBlank(message = "Project name is required")
        @Size(max = 120, message = "Project name must be less than 120 characters")
        @Schema(description = "项目名称", example = "电商项目")
        String name,

        @Size(max = 1000, message = "Project description must be less than 1000 characters")
        @Schema(description = "项目描述", example = "订单、支付和前端服务的 CI 项目")
        String description,

        @NotNull(message = "Code source id is required")
        @Schema(description = "已验证的代码源连接 ID", example = "1")
        Long codeSourceId
) {
}
