package com.pipeline.platform.project.domain;

import java.util.Optional;

public interface ProjectGitRepositoryRepository {

    ProjectGitRepository save(ProjectGitRepository repository);

    Optional<ProjectGitRepository> findByProjectId(Long projectId);
}
