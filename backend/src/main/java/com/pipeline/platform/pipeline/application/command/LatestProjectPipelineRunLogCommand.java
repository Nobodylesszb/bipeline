package com.pipeline.platform.pipeline.application.command;

public record LatestProjectPipelineRunLogCommand(
        Long projectId,
        Long pipelineId
) {
}
