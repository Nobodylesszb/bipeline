package com.pipeline.platform.project.application.service;

import com.pipeline.platform.project.application.command.CreateProjectCommand;
import com.pipeline.platform.project.application.model.ProjectView;
import com.pipeline.platform.project.domain.Project;
import com.pipeline.platform.project.domain.ProjectRepository;
import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import com.pipeline.platform.shared.time.ClockProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CreateProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private CodeSourceGuard codeSourceGuard;

    @Autowired
    private ClockProvider clockProvider;

    @Transactional(rollbackFor = Exception.class)
    public ProjectView create(CreateProjectCommand command) {
        if (projectRepository.existsByName(command.name())) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_CONFLICT,
                    "Project name already exists"
            );
        }

        codeSourceGuard.requireVerified(command.codeSourceId());
        Project project = Project.create(
                command.name(),
                command.description(),
                command.codeSourceId(),
                clockProvider.now()
        );

        return ProjectView.from(projectRepository.save(project), null);
    }
}
