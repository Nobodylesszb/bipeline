package com.pipeline.platform.source.application.model;

public record GitRepositoryInfo(
        String repositoryPath,
        String name,
        String fullName,
        String defaultBranch,
        String cloneUrl,
        String sshUrl
) {
}
