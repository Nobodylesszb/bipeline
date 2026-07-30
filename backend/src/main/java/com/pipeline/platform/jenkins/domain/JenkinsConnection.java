package com.pipeline.platform.jenkins.domain;

import java.time.OffsetDateTime;

import com.pipeline.platform.source.domain.VerificationStatus;

public record JenkinsConnection(
        Long id,
        String name,
        String baseUrl,
        String username,
        String apiTokenPlain,
        VerificationStatus verificationStatus,
        OffsetDateTime lastVerifiedAt,
        String lastVerificationMessage,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static JenkinsConnection create(
            String name,
            String baseUrl,
            String username,
            String apiTokenPlain,
            OffsetDateTime now
    ) {
        return new JenkinsConnection(
                null,
                name.trim(),
                normalizeBaseUrl(baseUrl),
                username.trim(),
                blankToNull(apiTokenPlain),
                VerificationStatus.UNVERIFIED,
                null,
                null,
                now,
                now
        );
    }

    public JenkinsConnection withVerification(
            VerificationStatus status,
            OffsetDateTime verifiedAt,
            String message
    ) {
        return new JenkinsConnection(
                id,
                name,
                baseUrl,
                username,
                apiTokenPlain,
                status,
                verifiedAt,
                message,
                createdAt,
                verifiedAt
        );
    }

    private static String normalizeBaseUrl(String baseUrl) {
        String trimmed = baseUrl.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
