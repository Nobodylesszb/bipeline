package com.pipeline.platform.project.application.model;

import java.time.OffsetDateTime;

import com.pipeline.platform.project.domain.Project;
import com.pipeline.platform.project.domain.ProjectStatus;

public record ProjectView(
        Long id,
        String name,
        String description,
        Long codeSourceId,
        ProjectStatus status,
        ProjectGitRepositoryView repository,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static ProjectView from(Project project, ProjectGitRepositoryView repository) {
        return new ProjectView(
                project.id(),
                project.name(),
                project.description(),
                project.codeSourceId(),
                project.status(),
                repository,
                project.createdAt(),
                project.updatedAt()
        );
    }
}
