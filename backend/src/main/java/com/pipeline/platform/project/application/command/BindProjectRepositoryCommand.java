package com.pipeline.platform.project.application.command;

public record BindProjectRepositoryCommand(
        Long projectId,
        Long codeSourceId,
        String repositoryPath,
        String defaultBranch,
        String contextDirectory
) {
}
