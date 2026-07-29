package com.pipeline.platform.project.domain;

import java.time.OffsetDateTime;

public record ProjectGitRepository(
        Long id,
        Long projectId,
        String remotePath,
        String remoteUrl,
        String defaultBranch,
        String contextDirectory,
        String lastResolvedRevision,
        OffsetDateTime lastFetchedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static ProjectGitRepository bind(
            Long projectId,
            String remotePath,
            String remoteUrl,
            String defaultBranch,
            String contextDirectory,
            String lastResolvedRevision,
            OffsetDateTime now
    ) {
        return new ProjectGitRepository(
                null,
                projectId,
                remotePath.trim(),
                remoteUrl,
                defaultBranch.trim(),
                normalizeContextDirectory(contextDirectory),
                lastResolvedRevision,
                now,
                now,
                now
        );
    }

    public ProjectGitRepository update(
            String remotePath,
            String remoteUrl,
            String defaultBranch,
            String contextDirectory,
            String lastResolvedRevision,
            OffsetDateTime now
    ) {
        return new ProjectGitRepository(
                id,
                projectId,
                remotePath.trim(),
                remoteUrl,
                defaultBranch.trim(),
                normalizeContextDirectory(contextDirectory),
                lastResolvedRevision,
                now,
                createdAt,
                now
        );
    }

    private static String normalizeContextDirectory(String contextDirectory) {
        if (contextDirectory == null || contextDirectory.isBlank()) {
            return ".";
        }
        return contextDirectory.trim();
    }
}
