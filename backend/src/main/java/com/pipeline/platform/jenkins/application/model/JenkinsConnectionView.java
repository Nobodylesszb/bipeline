package com.pipeline.platform.jenkins.application.model;

import java.time.OffsetDateTime;

import com.pipeline.platform.jenkins.domain.JenkinsConnection;
import com.pipeline.platform.source.domain.VerificationStatus;

public record JenkinsConnectionView(
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

    public static JenkinsConnectionView from(JenkinsConnection connection) {
        return new JenkinsConnectionView(
                connection.id(),
                connection.name(),
                connection.baseUrl(),
                connection.username(),
                connection.apiTokenPlain(),
                connection.verificationStatus(),
                connection.lastVerifiedAt(),
                connection.lastVerificationMessage(),
                connection.createdAt(),
                connection.updatedAt()
        );
    }
}
