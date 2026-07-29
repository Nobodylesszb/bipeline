package com.pipeline.platform.source.application;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.pipeline.platform.source.domain.AuthType;
import com.pipeline.platform.source.domain.CodeSource;
import com.pipeline.platform.source.domain.CodeSourceProvider;
import com.pipeline.platform.source.domain.VerificationStatus;

public record CodeSourceView(
        UUID id,
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

    public static CodeSourceView from(CodeSource codeSource) {
        return new CodeSourceView(
                codeSource.id(),
                codeSource.name(),
                codeSource.provider(),
                codeSource.baseUrl(),
                codeSource.authType(),
                codeSource.username(),
                codeSource.secretPlain(),
                codeSource.verificationStatus(),
                codeSource.lastVerifiedAt(),
                codeSource.lastVerificationMessage(),
                codeSource.createdAt(),
                codeSource.updatedAt()
        );
    }
}
