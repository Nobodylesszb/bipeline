package com.pipeline.platform.project.application.service;

import java.util.List;

import com.pipeline.platform.project.application.command.BindProjectRepositoryCommand;
import com.pipeline.platform.project.application.model.ProjectGitRepositoryView;
import com.pipeline.platform.project.domain.Project;
import com.pipeline.platform.project.domain.ProjectGitRepository;
import com.pipeline.platform.project.domain.ProjectGitRepositoryRepository;
import com.pipeline.platform.project.domain.ProjectRepository;
import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import com.pipeline.platform.shared.time.ClockProvider;
import com.pipeline.platform.source.application.model.GitBranchInfo;
import com.pipeline.platform.source.application.model.GitRepositoryInfo;
import com.pipeline.platform.source.application.port.GitProviderClient;
import com.pipeline.platform.source.domain.CodeSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BindProjectRepositoryService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectGitRepositoryRepository projectGitRepositoryRepository;

    @Autowired
    private CodeSourceGuard codeSourceGuard;

    @Autowired
    private GitProviderResolver gitProviderResolver;

    @Autowired
    private ClockProvider clockProvider;

    @Transactional(rollbackFor = Exception.class)
    public ProjectGitRepositoryView bind(BindProjectRepositoryCommand command) {
        Project project = projectRepository.findById(command.projectId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Project not found"
                ));
        ensureProjectCanUseCodeSource(project, command.codeSourceId());

        CodeSource codeSource = codeSourceGuard.requireVerified(command.codeSourceId());
        GitProviderClient gitProviderClient = gitProviderResolver.resolve(codeSource);
        GitRepositoryInfo repositoryInfo = gitProviderClient.getRepository(
                codeSource,
                command.repositoryPath()
        );
        List<GitBranchInfo> branches = gitProviderClient.listBranches(
                codeSource,
                command.repositoryPath()
        );
        String revision = revisionFor(command.defaultBranch(), branches);

        ProjectGitRepository repository = projectGitRepositoryRepository.findByProjectId(project.id())
                .map(existing -> existing.update(
                        repositoryInfo.repositoryPath(),
                        repositoryInfo.cloneUrl(),
                        command.defaultBranch(),
                        command.contextDirectory(),
                        revision,
                        clockProvider.now()
                ))
                .orElseGet(() -> ProjectGitRepository.bind(
                        project.id(),
                        repositoryInfo.repositoryPath(),
                        repositoryInfo.cloneUrl(),
                        command.defaultBranch(),
                        command.contextDirectory(),
                        revision,
                        clockProvider.now()
                ));

        return ProjectGitRepositoryView.from(projectGitRepositoryRepository.save(repository));
    }

    private void ensureProjectCanUseCodeSource(Project project, Long codeSourceId) {
        if (!project.active()) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_CONFLICT,
                    "Project is not active"
            );
        }
        if (!project.usesCodeSource(codeSourceId)) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_CONFLICT,
                    "Project code source does not match request"
            );
        }
    }

    private String revisionFor(String defaultBranch, List<GitBranchInfo> branches) {
        return branches.stream()
                .filter(branch -> branch.name().equals(defaultBranch))
                .findFirst()
                .map(GitBranchInfo::commitId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.REPOSITORY_NOT_ACCESSIBLE,
                        "Default branch is not accessible"
                ));
    }
}
