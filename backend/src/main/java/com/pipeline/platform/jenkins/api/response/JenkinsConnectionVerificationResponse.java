package com.pipeline.platform.jenkins.api.response;

import java.time.OffsetDateTime;

import com.pipeline.platform.source.domain.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Jenkins 连接验证响应")
public record JenkinsConnectionVerificationResponse(
        VerificationStatus status,
        String message,
        OffsetDateTime verifiedAt
) {
}
