package com.pipeline.platform.jenkins.infrastructure.persistence;

import java.time.OffsetDateTime;

import com.pipeline.platform.jenkins.domain.JenkinsConnection;
import com.pipeline.platform.source.domain.VerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "jenkins_connections")
class JenkinsConnectionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @Column(nullable = false, length = 200)
    private String username;

    @Column(name = "api_token_plain")
    private String apiTokenPlain;

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

    protected JenkinsConnectionJpaEntity() {
    }

    static JenkinsConnectionJpaEntity from(JenkinsConnection connection) {
        JenkinsConnectionJpaEntity entity = new JenkinsConnectionJpaEntity();
        entity.id = connection.id();
        entity.name = connection.name();
        entity.baseUrl = connection.baseUrl();
        entity.username = connection.username();
        entity.apiTokenPlain = connection.apiTokenPlain();
        entity.verificationStatus = connection.verificationStatus();
        entity.lastVerifiedAt = connection.lastVerifiedAt();
        entity.lastVerificationMessage = connection.lastVerificationMessage();
        entity.createdAt = connection.createdAt();
        entity.updatedAt = connection.updatedAt();
        return entity;
    }

    JenkinsConnection toDomain() {
        return new JenkinsConnection(
                id,
                name,
                baseUrl,
                username,
                apiTokenPlain,
                verificationStatus,
                lastVerifiedAt,
                lastVerificationMessage,
                createdAt,
                updatedAt
        );
    }
}
