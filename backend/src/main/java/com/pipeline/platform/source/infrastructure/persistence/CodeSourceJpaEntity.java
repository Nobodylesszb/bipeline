package com.pipeline.platform.source.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.pipeline.platform.source.domain.AuthType;
import com.pipeline.platform.source.domain.CodeSource;
import com.pipeline.platform.source.domain.CodeSourceProvider;
import com.pipeline.platform.source.domain.VerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "code_sources")
class CodeSourceJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CodeSourceProvider provider;

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", nullable = false, length = 32)
    private AuthType authType;

    @Column(length = 200)
    private String username;

    @Column(name = "secret_plain")
    private String secretPlain;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 32)
    private VerificationStatus verificationStatus;

    @Column(name = "last_verified_at")
    private OffsetDateTime lastVerifiedAt;

    @Column(name = "last_verification_message", length = 1000)
    private String lastVerificationMessage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected CodeSourceJpaEntity() {
    }

    static CodeSourceJpaEntity from(CodeSource codeSource) {
        CodeSourceJpaEntity entity = new CodeSourceJpaEntity();
        entity.id = codeSource.id();
        entity.name = codeSource.name();
        entity.provider = codeSource.provider();
        entity.baseUrl = codeSource.baseUrl();
        entity.authType = codeSource.authType();
        entity.username = codeSource.username();
        entity.secretPlain = codeSource.secretPlain();
        entity.verificationStatus = codeSource.verificationStatus();
        entity.lastVerifiedAt = codeSource.lastVerifiedAt();
        entity.lastVerificationMessage = codeSource.lastVerificationMessage();
        entity.createdAt = codeSource.createdAt();
        entity.updatedAt = codeSource.updatedAt();
        return entity;
    }

    CodeSource toDomain() {
        return new CodeSource(
                id,
                name,
                provider,
                baseUrl,
                authType,
                username,
                secretPlain,
                verificationStatus,
                lastVerifiedAt,
                lastVerificationMessage,
                createdAt,
                updatedAt
        );
    }
}
