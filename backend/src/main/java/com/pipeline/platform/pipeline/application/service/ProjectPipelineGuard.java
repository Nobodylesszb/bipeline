package com.pipeline.platform.pipeline.application.service;

import com.pipeline.platform.project.domain.Project;
import com.pipeline.platform.project.domain.ProjectGitRepository;
import com.pipeline.platform.project.domain.ProjectGitRepositoryRepository;
import com.pipeline.platform.project.domain.ProjectRepository;
import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class ProjectPipelineGuard {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectGitRepositoryRepository projectGitRepositoryRepository;

    ProjectGitRepository requireActiveProjectWithRepository(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Project not found"
                ));
        if (!project.active()) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_CONFLICT,
                    "Project is not active"
            );
        }
        return projectGitRepositoryRepository.findByProjectId(project.id())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_CONFLICT,
                        "Project repository is not bound"
                ));
    }
}
