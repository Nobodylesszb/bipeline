package com.pipeline.platform.project.application.command;

public record VerifyRepositoryCommand(
        Long codeSourceId,
        String repositoryPath
) {
}
