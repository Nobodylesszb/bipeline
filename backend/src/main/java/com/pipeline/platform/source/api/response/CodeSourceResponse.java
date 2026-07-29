package com.pipeline.platform.source.api.response;

import java.time.OffsetDateTime;

import com.pipeline.platform.source.domain.AuthType;
import com.pipeline.platform.source.domain.CodeSourceProvider;
import com.pipeline.platform.source.domain.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "代码源连接响应")
public record CodeSourceResponse(
        @Schema(description = "代码源连接 ID")
        Long id,
        @Schema(description = "代码源名称")
        String name,
        @Schema(description = "代码平台类型")
        CodeSourceProvider provider,
        @Schema(description = "代码平台地址")
        String baseUrl,
        @Schema(description = "认证方式")
        AuthType authType,
        @Schema(description = "用户名")
        String username,
        @Schema(description = "脱敏后的密钥固定展示值", example = "********")
        String secretMasked,
        @Schema(description = "密钥最后四位，用于用户识别当前配置", example = "abcd")
        String secretLastFour,
        @Schema(description = "连通性验证状态")
        VerificationStatus verificationStatus,
        @Schema(description = "最后验证时间")
        OffsetDateTime lastVerifiedAt,
        @Schema(description = "最后验证消息")
        String lastVerificationMessage,
        @Schema(description = "创建时间")
        OffsetDateTime createdAt,
        @Schema(description = "更新时间")
        OffsetDateTime updatedAt
) {
}
