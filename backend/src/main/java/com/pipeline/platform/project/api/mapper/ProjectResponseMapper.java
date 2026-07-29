package com.pipeline.platform.project.api.mapper;

import com.pipeline.platform.project.api.response.ProjectGitRepositoryResponse;
import com.pipeline.platform.project.api.response.ProjectResponse;
import com.pipeline.platform.project.api.response.RepositoryBranchResponse;
import com.pipeline.platform.project.api.response.RepositoryVerificationResponse;
import com.pipeline.platform.project.application.model.ProjectGitRepositoryView;
import com.pipeline.platform.project.application.model.ProjectView;
import com.pipeline.platform.project.application.model.RepositoryBranchView;
import com.pipeline.platform.project.application.model.RepositoryVerificationView;
import org.springframework.stereotype.Component;

@Component
public class ProjectResponseMapper {

    public ProjectResponse toResponse(ProjectView view) {
        return new ProjectResponse(
                view.id(),
                view.name(),
                view.description(),
                view.codeSourceId(),
                view.status(),
                toRepositoryResponse(view.repository()),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public ProjectGitRepositoryResponse toRepositoryResponse(ProjectGitRepositoryView view) {
        if (view == null) {
            return null;
        }
        return new ProjectGitRepositoryResponse(
                view.id(),
                view.projectId(),
                view.remotePath(),
                view.remoteUrl(),
                view.defaultBranch(),
                view.contextDirectory(),
                view.lastResolvedRevision(),
                view.lastFetchedAt(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public RepositoryVerificationResponse toRepositoryVerificationResponse(RepositoryVerificationView view) {
        return new RepositoryVerificationResponse(
                view.repositoryPath(),
                view.name(),
                view.fullName(),
                view.defaultBranch(),
                view.cloneUrl(),
                view.sshUrl(),
                view.accessible()
        );
    }

    public RepositoryBranchResponse toBranchResponse(RepositoryBranchView view) {
        return new RepositoryBranchResponse(
                view.name(),
                view.commitId()
        );
    }
}
