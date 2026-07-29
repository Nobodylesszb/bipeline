package com.pipeline.platform.source.application.command;

public record VerifyCodeSourceCommand(
        Long codeSourceId,
        String repositoryPath
) {
}
