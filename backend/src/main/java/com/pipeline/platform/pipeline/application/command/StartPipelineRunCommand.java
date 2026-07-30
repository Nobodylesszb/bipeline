package com.pipeline.platform.pipeline.application.command;

public record StartPipelineRunCommand(
        Long pipelineId,
        Long jenkinsConnectionId,
        String branch,
        String commitSha
) {
}
