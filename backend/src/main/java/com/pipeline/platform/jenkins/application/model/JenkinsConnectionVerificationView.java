package com.pipeline.platform.jenkins.application.model;

import java.time.OffsetDateTime;

import com.pipeline.platform.source.domain.VerificationStatus;

public record JenkinsConnectionVerificationView(
        VerificationStatus status,
        String message,
        OffsetDateTime verifiedAt
) {
}
