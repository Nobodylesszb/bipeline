package com.pipeline.platform.project.application.command;

public record ListRepositoryBranchesCommand(
        Long codeSourceId,
        String repositoryPath
) {
}
