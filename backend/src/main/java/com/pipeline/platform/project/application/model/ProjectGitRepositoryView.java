package com.pipeline.platform.project.application.model;

import java.time.OffsetDateTime;

import com.pipeline.platform.project.domain.ProjectGitRepository;

public record ProjectGitRepositoryView(
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

    public static ProjectGitRepositoryView from(ProjectGitRepository repository) {
        return new ProjectGitRepositoryView(
                repository.id(),
                repository.projectId(),
                repository.remotePath(),
                repository.remoteUrl(),
                repository.defaultBranch(),
                repository.contextDirectory(),
                repository.lastResolvedRevision(),
                repository.lastFetchedAt(),
                repository.createdAt(),
                repository.updatedAt()
        );
    }
}
