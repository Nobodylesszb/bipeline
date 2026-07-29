package com.pipeline.platform.source.application.model;

import java.time.OffsetDateTime;

import com.pipeline.platform.source.domain.VerificationStatus;

public record CodeSourceVerificationView(
        VerificationStatus status,
        String message,
        GitProviderCapabilities capabilities,
        OffsetDateTime verifiedAt
) {
}
