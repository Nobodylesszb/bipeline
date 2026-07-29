package com.pipeline.platform.source.api.response;

import java.time.OffsetDateTime;

import com.pipeline.platform.source.application.model.GitProviderCapabilities;
import com.pipeline.platform.source.domain.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "代码源连通性验证响应")
public record CodeSourceVerificationResponse(
        @Schema(description = "验证状态", example = "VERIFIED")
        VerificationStatus status,
        @Schema(description = "验证消息", example = "GitLab code source is accessible")
        String message,
        @Schema(description = "代码源能力")
        GitProviderCapabilities capabilities,
        @Schema(description = "验证时间")
        OffsetDateTime verifiedAt
) {
}
