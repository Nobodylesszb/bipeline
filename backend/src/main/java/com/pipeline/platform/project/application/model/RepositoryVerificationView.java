package com.pipeline.platform.project.application.model;

import com.pipeline.platform.source.application.model.GitRepositoryInfo;

public record RepositoryVerificationView(
        String repositoryPath,
        String name,
        String fullName,
        String defaultBranch,
        String cloneUrl,
        String sshUrl,
        boolean accessible
) {

    public static RepositoryVerificationView accessible(GitRepositoryInfo repositoryInfo) {
        return new RepositoryVerificationView(
                repositoryInfo.repositoryPath(),
                repositoryInfo.name(),
                repositoryInfo.fullName(),
                repositoryInfo.defaultBranch(),
                repositoryInfo.cloneUrl(),
                repositoryInfo.sshUrl(),
                true
        );
    }
}
