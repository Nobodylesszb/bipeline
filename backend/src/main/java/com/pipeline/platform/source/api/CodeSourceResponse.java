package com.pipeline.platform.source.api;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.pipeline.platform.source.domain.AuthType;
import com.pipeline.platform.source.domain.CodeSourceProvider;
import com.pipeline.platform.source.domain.VerificationStatus;

public record CodeSourceResponse(
        UUID id,
        String name,
        CodeSourceProvider provider,
        String baseUrl,
        AuthType authType,
        String username,
        String secretMasked,
        String secretLastFour,
        VerificationStatus verificationStatus,
        OffsetDateTime lastVerifiedAt,
        String lastVerificationMessage,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
