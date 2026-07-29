package com.pipeline.platform.project.application.command;

public record CreateProjectCommand(
        String name,
        String description,
        Long codeSourceId
) {
}
