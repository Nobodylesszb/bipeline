package com.pipeline.platform.jenkins.api.response;

import java.time.OffsetDateTime;

import com.pipeline.platform.source.domain.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Jenkins 连接响应")
public record JenkinsConnectionResponse(
        Long id,
        String name,
        String baseUrl,
        String username,
        String apiTokenMasked,
        String apiTokenLastFour,
        VerificationStatus verificationStatus,
        OffsetDateTime lastVerifiedAt,
        String lastVerificationMessage,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
