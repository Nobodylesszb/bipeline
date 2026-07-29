package com.pipeline.platform.source.domain;

import java.time.OffsetDateTime;

public record CodeSource(
        Long id,
        String name,
        CodeSourceProvider provider,
        String baseUrl,
        AuthType authType,
        String username,
        String secretPlain,
        VerificationStatus verificationStatus,
        OffsetDateTime lastVerifiedAt,
        String lastVerificationMessage,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static CodeSource create(
            Long id,
            String name,
            CodeSourceProvider provider,
            String baseUrl,
            AuthType authType,
            String username,
            String secretPlain,
            OffsetDateTime now
    ) {
        return new CodeSource(
                id,
                name,
                provider,
                normalizeBaseUrl(baseUrl),
                authType,
                blankToNull(username),
                blankToNull(secretPlain),
                VerificationStatus.UNVERIFIED,
                null,
                null,
                now,
                now
        );
    }

    public CodeSource withVerification(
            VerificationStatus status,
            OffsetDateTime verifiedAt,
            String message
    ) {
        return new CodeSource(
                id,
                name,
                provider,
                baseUrl,
                authType,
                username,
                secretPlain,
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
